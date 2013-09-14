import java.io.{ File, FileNotFoundException, IOException }
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.{ OWNER_EXECUTE, OWNER_READ }
import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters.setAsJavaSetConverter
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration

import org.specs2.mutable.{ After, Specification }

import com.lunatech.queue.{ DirectoryBackedQueue, Serializable }

class DirectoryBackedQueueSpec extends Specification {

  "An empty Queue" should {
    "return None" in new QueueScope {
      queue.dequeue must_== None
    }

    "allow a value to be queued" in new QueueScope {
      queue.enqueue("Foo!")
    }
  }

  "A Queue with one element" should {
    "return and remove the element on dequeue" in new QueueScope {
      queue.enqueue("Buzzz")
      queue.dequeue must_== Some("Buzzz")
      queue.dequeue must_== None
    }
  }

  "A Queue" should {
    "dequeue elements in a FIFO order" in new QueueScope {
      queue.enqueue("foo")
      queue.enqueue("bar")
      queue.enqueue("baz")
      (queue.dequeue(), queue.dequeue(), queue.dequeue()) must_==
        (Some("foo"), Some("bar"), Some("baz"))
    }
  }

  "A DirectoryBackedQueue" should {
    "ignore deleted files after they are cached" in new QueueScope {
      queue.enqueue("foo")
      queue.enqueue("bar")
      queue.enqueue("buzz")
      queue.dequeue() must_== Some("foo")
      emptyDirectory()
      queue.dequeue must_== None
    }

    "ignore hidden files" in new QueueScope {
      val hiddenFilePath = directory resolve ".hidden"
      val fileContent = implicitly[Serializable[String]].serialize("baz")
      Files.write(hiddenFilePath, fileContent)
      queue.dequeue must_== None
    }

    "pickup manually placed files" in new QueueScope {
      val hiddenFilePath = directory resolve ".hidden"
      val nonHiddenFilePath = directory resolve "unhidden"
      val fileContent = implicitly[Serializable[String]].serialize("baz")
      Files.write(hiddenFilePath, fileContent)
      Files.move(hiddenFilePath, nonHiddenFilePath)
      queue.dequeue must_== Some("baz")
    }

    "pickup elements queued in a different DirectoryBackedQueue backed by the same directory" in new QueueScope {
      val hiddenFilePath = directory resolve ".hidden"
      val nonHiddenFilePath = directory resolve "unhidden"
      val fileContent = implicitly[Serializable[String]].serialize("baz")
      Files.write(hiddenFilePath, fileContent)
      Files.move(hiddenFilePath, nonHiddenFilePath)
      queue.dequeue must_== Some("baz")
    }

    "throw a FileNotFound exception on construction when the directory doesn't exist" in {
      val directory = new File(".").toPath resolve "nonexistent"
      DirectoryBackedQueue[String](directory) must throwA[FileNotFoundException]
    }

    "throw an IOException on enqueue if it can't store a file in the directory" in new QueueScope {
      val oldPermissions = Files.getPosixFilePermissions(directory)
      Files.setPosixFilePermissions(directory, Set().asJava)
      queue.enqueue("foo") must throwA[IOException]
      Files.setPosixFilePermissions(directory, oldPermissions)
    }

    "throw an IOException on dequeue if it can't read the directory" in new QueueScope {
      queue.enqueue("foo")
      val oldPermissions = Files.getPosixFilePermissions(directory)
      Files.setPosixFilePermissions(directory, Set().asJava)
      queue.dequeue() must throwA[IOException]
      Files.setPosixFilePermissions(directory, oldPermissions)
    }

    "throw an IOException on dequeue if it can't read the file in the directory" in new QueueScope {
      queue.enqueue("foo")
      val files = directory.toFile.listFiles.filter { _.isFile() }.map { _.toPath }
      files must have size (1)
      files.headOption.map { file =>
        val oldPermissions = Files.getPosixFilePermissions(file)
        Files.setPosixFilePermissions(file, Set().asJava)
        queue.dequeue() must throwA[IOException]
        Files.setPosixFilePermissions(file, oldPermissions)

      }
    }

    "throw an IOException on dequeue if it can't delete the file from the directory" in new QueueScope {
      queue.enqueue("foo")
      val files = directory.toFile.listFiles.filter { _.isFile() }.map { _.toPath }
      files must have size (1)
      files.headOption.map { file =>
        val oldPermissions = Files.getPosixFilePermissions(directory)
        // Revoke permission to delete the file by not setting OWNER_WRITE on the *directory*.
        Files.setPosixFilePermissions(directory, Set(OWNER_READ, OWNER_EXECUTE).asJava)
        queue.dequeue() must throwA[IOException]
        Files.setPosixFilePermissions(directory, oldPermissions)
      }
    }
  }

  "Multiple threads calling dequeue on the same DirectoryBackedQueue" should {

    // Insert N elements into a queue, and dequeue concurrently from M
    // threads. All threads together should have dequeued only N elements.
    "not get duplicate elements" in new QueueScope {
      val threads = 2
      val elements = 2000
      val elementSize = 10000
      val elementContent = "a" * elementSize
      1 to elements foreach { _ => queue.enqueue(elementContent) }

      val countsPerThreadFutures = (1 to threads).map { _ =>
        Future {
          (1 to elements).foldLeft(0) { case (count, _) => queue.dequeue().map { _ => count + 1 } getOrElse count }
        }
      }

      val countsPerThread = Await.result(Future.sequence(countsPerThreadFutures), FiniteDuration(3, TimeUnit.SECONDS))
      val totalDequeued = countsPerThread.sum

      totalDequeued must_== elements
    }
  }

  "Multiple threads calling dequeue on their own DirectoryBackedQueue" should {

    "not get duplicate elements" in new QueueScope {
      val threads = 2
      val elements = 2000
      val elementSize = 10000
      val elementContent = "a" * elementSize
      1 to elements foreach { _ => queue.enqueue(elementContent) }

      val countsPerThreadFutures = (1 to threads).map { _ =>
        val queueForThread = DirectoryBackedQueue(directory)
        Future {
          (1 to elements).foldLeft(0) { case (count, _) => queueForThread.dequeue().map { _ => count + 1 } getOrElse count }
        }
      }

      val countsPerThread = Await.result(Future.sequence(countsPerThreadFutures), FiniteDuration(3, TimeUnit.SECONDS))
      val totalDequeued = countsPerThread.sum

      totalDequeued must_== elements
    }
  }

  trait QueueScope extends After {
    val directory = Files.createTempDirectory("queue-")
    val queue = DirectoryBackedQueue[String](directory)
    def emptyDirectory() = directory.toFile.listFiles().foreach { _.delete() }

    override def after() = {
      emptyDirectory()
      Files.delete(directory)
    }
  }
}
