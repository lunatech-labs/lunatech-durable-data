package com.lunatech.queue

import java.io.{ File, FileFilter, FileNotFoundException, IOException }
import java.nio.file.{ Files, NoSuchFileException, Path }
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardOpenOption.{ CREATE_NEW, SYNC }

import scala.Array.canBuildFrom
import scala.collection.immutable.{ Queue => ScalaQueue }

import com.fasterxml.uuid.{NoArgGenerator, Generators}

/**
 * Directory backed persistent queue.
 *
 * A [[Queue]] implementation that stores elements in the queue as files in a
 * directory. Files are named with a time-based UUID.
 *
 * All methods on this queue are thread safe, and multiple instances can be created
 * with the same backing directory.
 *
 * ==Storage details==
 * Files that are not hidden (filename doesn't start with '.') are picked up as
 * elements of the queue.
 *
 * When an element is written to disk, it's first saved with a hidden filename
 * and then moved to its final name. The move operation is atomic on POSIX systems.
 *
 * @tparam E Element type of this queue
 */
class DirectoryBackedQueue[E] private (serializer: Serializable[E], backingDirectory: Path) extends Queue[E] {

  /**
   * List of files in the backing directory that existed at the last directory
   * scan and haven't been dequeued yet.
   *
   * Note that some of the files might not exist
   */
  private var cachedFiles: ScalaQueue[Path] = ScalaQueue()

  /**
   * Generator for time-based uuids
   */
  protected val uuidGenerator: NoArgGenerator = Generators.timeBasedGenerator

  /**
   * Filter that selects the files are elements of the queue
   */
  protected val elementFileFilter = new FileFilter {
    def accept(file: File) = file.isFile() && !file.isHidden
  }

  if (!Files.isDirectory(backingDirectory))
    throw new FileNotFoundException("Directory " + backingDirectory.toString + " does not exist or is not accessible")

  override def enqueue(element: E): String = enqueueBytes(serializer serialize element)

  /**
   * Enqueue an element as bytes.
   * Writes bytes to a hidden file in the backing directory and does an atomic
   * move to a non hidden file when ready.
   */
  protected def enqueueBytes(bytes: Array[Byte]) = {
    val finalFilename = uuidGenerator.generate.toString
    val finalFile = backingDirectory resolve finalFilename
    val tmpFilename = "." + finalFilename
    val tmpFile = backingDirectory resolve tmpFilename
    Files.write(tmpFile, bytes, CREATE_NEW, SYNC)
    Files.move(tmpFile, finalFile, ATOMIC_MOVE)
    finalFilename
  }

  override def dequeue(): Option[E] = dequeueBytes() map { serializer.deserialize }

  /**
   * Dequeue an element and return the bytes.
   *
   * Relies on Files.delete to throw an exception if the file is already deleted.
   */
  protected def dequeueBytes(): Option[Array[Byte]] =
    nextFile() flatMap { file =>
      try {
        val bytes = Files.readAllBytes(file)
        Files.delete(file)
        val p = new sun.nio.fs.BsdFileSystemProvider
        Some(bytes)
      } catch {
        case e: NoSuchFileException => dequeueBytes()
      }
    }

  /**
   * Find the next file that is a queue element.
   *
   * Uses caching to reduce the required number of directory scans. Sorts files
   * by filename, which is by enqueue order because of the timestamp based UUID
   * filename.
   *
   * This method is synchronized.
   */
  protected def nextFile(): Option[Path] = synchronized {
    if (cachedFiles.isEmpty) cachedFiles ++= findFiles
    if (cachedFiles.isEmpty) None
    else {
      val (nextFile, newCachedFiles) = cachedFiles.dequeue
      cachedFiles = newCachedFiles
      Some(nextFile)
    }
  }

  /**
   * List files in the backing directory.
   */
  protected def findFiles = try {
    backingDirectory.toFile.listFiles(elementFileFilter).sorted.map { _.toPath }
  } catch {
    // We've observed NPE's when there are no permissions on the directory.
    // We expect IOExceptions instead of NPE's in that case, so we wrap the NPE and rethrow.
    case e: NullPointerException => throw new IOException("Failed to list files in queue directory", e)
  }

}

/**
 * This object provides operations to create [[DirectoryBackedQueue]] instances.
 */
object DirectoryBackedQueue {
  /**
   * Create a queue on top of a backing directory.
   *
   * Requires an instance of the [[Serializable]] typeclass for the element type
   * of this queue.
   *
   * The backing directory must already exist and this process must have
   * rwx permissions.
   *
   * Multiple instances of Queue can be created on the same backing directory,
   * they will then share elements.
   *
   * @tparam E Element type of the queue
   * @param backingDirectory The directory backing this queue. Must already exist.
   */
  def apply[E: Serializable](backingDirectory: Path) =
    new DirectoryBackedQueue[E](implicitly[Serializable[E]], backingDirectory)
}
