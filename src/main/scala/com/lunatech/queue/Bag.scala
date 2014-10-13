package com.lunatech.queue

trait Bag[E] {
  /**
   * Insert an element into the bag. Returns the created key.
   */
  def insert(element: E): String

  /**
   * Remove an element from the bag.
   *
   * Throws a NoSuchElementException if the key doesn't exist.
   */
  def remove(key: String): Unit

  /**
   * Get all key/element pairs in a map
   */
  def list: Map[String, E]

}