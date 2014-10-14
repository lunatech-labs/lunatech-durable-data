package com.lunatech.durable

import java.io.{ File, FileNotFoundException, IOException }
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.{ OWNER_EXECUTE, OWNER_READ }
import org.specs2.mutable.{ After, Specification }
import scala.collection.JavaConverters.setAsJavaSetConverter
import scala.io.Codec

class DirectoryBackedBagSpec extends Specification {
  "An empty Bag" should {
    "return an empty map on 'list'" in new BagScope {
      bag.list must_== Map.empty
    }

    "allow a value to be added" in new BagScope {
      bag.insert("Foo!")
    }

    "throw a NoSuchElementException when removing a non-existing key" in new BagScope {
      bag.remove("fake") must throwA[NoSuchElementException]
    }
  }

  "A Queue with one element" should {
    "return a map with the element on 'list'" in new BagScope {
      val key = bag.insert("Buzzz")
      bag.list must_== Map(key -> "Buzzz")
    }

    "remove the element on 'remove'" in new BagScope {
      val key = bag.insert("Quux")
      bag.list must haveSize(1)
      bag.remove(key)
      bag.list must_== Map.empty
    }
  }

  "A Byte Array Bag" should {
    "handle a byte array" in new ByteBagScope {
      val foo: Array[Byte] = "FOO".getBytes(Codec.UTF8.charSet)
      val key = bag.insert(foo)
      bag.list.values.map { _.toList } must_== List(foo.toList)
    }
  }

  "A DirectoryBackedBag" should {
    "ignore hidden files" in new BagScope {
      val hiddenFilePath = directory resolve ".hidden"
      val fileContent = implicitly[Serializable[String]].serialize("baz")
      Files.write(hiddenFilePath, fileContent)
      bag.list must_== Map.empty
    }

    "pickup manually placed files" in new BagScope {
      val hiddenFilePath = directory resolve ".hidden"
      val nonHiddenFilePath = directory resolve "unhidden"
      val fileContent = implicitly[Serializable[String]].serialize("baz")
      Files.write(hiddenFilePath, fileContent)
      Files.move(hiddenFilePath, nonHiddenFilePath)
      bag.list must_== Map("unhidden" -> "baz")
    }

    "pickup elements bagged in a different DirectoryBackedBag backed by the same directory" in new BagScope {
      val otherBag = DirectoryBackedBag[String](directory)
      val key = otherBag.insert("foo")
      bag.list must_== Map(key -> "foo")
    }

    "throw a FileNotFound exception on construction when the directory doesn't exist" in {
      val directory = new File(".").toPath resolve "nonexistent"
      DirectoryBackedBag[String](directory) must throwA[FileNotFoundException]
    }

    "throw an IOException on insert if it can't store a file in the directory" in new BagScope {
      val oldPermissions = Files.getPosixFilePermissions(directory)
      Files.setPosixFilePermissions(directory, Set().asJava)
      bag.insert("foo") must throwA[IOException]
      Files.setPosixFilePermissions(directory, oldPermissions)
    }

    "throw an IOException on remove if it can't read the directory" in new BagScope {
      val key = bag.insert("foo")
      val oldPermissions = Files.getPosixFilePermissions(directory)
      Files.setPosixFilePermissions(directory, Set().asJava)
      bag.remove(key) must throwA[IOException]
      Files.setPosixFilePermissions(directory, oldPermissions)
    }

    "throw an IOException on remove if it can't delete the file from the directory" in new BagScope {
      val key = bag.insert("foo")
      val files = directory.toFile.listFiles.filter { _.isFile() }.map { _.toPath }
      files must have size (1)
      files.headOption.map { file =>
        val oldPermissions = Files.getPosixFilePermissions(directory)
        // Revoke permission to delete the file by not setting OWNER_WRITE on the *directory*.
        Files.setPosixFilePermissions(directory, Set(OWNER_READ, OWNER_EXECUTE).asJava)
        bag.remove(key) must throwA[IOException]
        Files.setPosixFilePermissions(directory, oldPermissions)
      }
    }
  }

  trait BagScope extends After {
    val directory = Files.createTempDirectory("bag-")
    val bag = DirectoryBackedBag[String](directory)
    def emptyDirectory() = directory.toFile.listFiles().foreach { _.delete() }

    override def after() = {
      emptyDirectory()
      Files.delete(directory)
    }
  }

  trait ByteBagScope extends After {
    val directory = Files.createTempDirectory("bag-")
    val bag = DirectoryBackedBag[Array[Byte]](directory)
    def emptyDirectory() = directory.toFile.listFiles().foreach { _.delete() }

    override def after() = {
      emptyDirectory()
      Files.delete(directory)
    }
  }
}
