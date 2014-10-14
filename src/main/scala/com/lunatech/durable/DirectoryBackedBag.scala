package com.lunatech.durable

import com.fasterxml.uuid.{ Generators, NoArgGenerator }
import java.io.{ File, FileFilter }
import java.nio.file.{ Files, NoSuchFileException, Path }
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardOpenOption.{ CREATE_NEW, SYNC }
import java.io.FileNotFoundException

/**
 * Durable Bag that stores elements on disk.
 *
 * Useful for building at-least-once delivery systems that survive application restarts.
 *
 * The `list` method is not thread safe. The normal use case is to use `list` only at application
 * start and wait for it to complete.
 */
class DirectoryBackedBag[E] private (serializer: Serializable[E], backingDirectory: Path) extends Bag[E] {

  /**
   * Generator for uuids
   */
  protected val uuidGenerator: NoArgGenerator = Generators.randomBasedGenerator()

  /**
   * Filter that selects the files are elements of the bag
   */
  protected val elementFileFilter = new FileFilter {
    def accept(file: File) = file.isFile() && !file.isHidden
  }

  if (!Files.isDirectory(backingDirectory))
    throw new FileNotFoundException("Directory " + backingDirectory.toString + " does not exist or is not accessible")

  override def insert(element: E): String = insertBytes(serializer serialize element)

  /**
   * Store a serialized element on disk. Uses atomicity
   * Writes bytes to a hidden file in the backing directory and does an atomic
   * move to a non hidden file when ready.
   */
  protected def insertBytes(bytes: Array[Byte]) = {
    val finalFilename = uuidGenerator.generate.toString
    val finalFile = backingDirectory resolve finalFilename
    val tmpFilename = "." + finalFilename
    val tmpFile = backingDirectory resolve tmpFilename
    Files.write(tmpFile, bytes, CREATE_NEW, SYNC)
    Files.move(tmpFile, finalFile, ATOMIC_MOVE)
    finalFilename
  }

  /**
   * Remove an element from the bag.
   *
   * Throws a NoSuchElementException if the key doesn't exist.
   *
   * Relies on Files.delete to throw an NoSuchFileException if the file is already deleted.
   */
  override def remove(key: String): Unit = {
    val file = backingDirectory resolve key
    try {
      Files.delete(file)
    } catch {
      case _: NoSuchFileException => throw new NoSuchElementException
    }
  }

  /**
   * List elements in the bag.
   *
   * NOT THREAD SAFE!
   */
  def list: Map[String, E] = {
    val files = backingDirectory.toFile.listFiles(elementFileFilter)
    files.map { file =>
      val key = file.getName
      val valueBytes = Files.readAllBytes(file.toPath)
      val value = serializer deserialize valueBytes
      key -> value
    }.toMap
  }

}

object DirectoryBackedBag {
  def apply[E: Serializable](backingDirectory: Path) =
    new DirectoryBackedBag[E](implicitly[Serializable[E]], backingDirectory)
}
