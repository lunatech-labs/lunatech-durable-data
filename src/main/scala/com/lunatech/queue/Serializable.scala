package com.lunatech.queue

import scala.io.Codec

/**
 * Type class for serialization to byte arrays and back.
 */
trait Serializable[E] {

  /**
   * Serialize an element to a byte array
   */
  def serialize(element: E): Array[Byte]

  /**
   * Deserialize a byte array to an object
   */
  def deserialize(bytes: Array[Byte]): E
}

/**
 * Default Serializable implementations
 */
trait DefaultSerializables {

  /**
   * Serializable for String. Encodes with UTF8 character set.
   */
  implicit val StringSerializable = new Serializable[String] {
    def serialize(element: String) = element.getBytes(Codec.UTF8.charSet)
    def deserialize(bytes: Array[Byte]) = new String(bytes, Codec.UTF8.charSet)
  }
}

object Serializable extends DefaultSerializables