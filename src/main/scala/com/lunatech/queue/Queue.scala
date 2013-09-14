package com.lunatech.queue

/**
 * Minimal queue interface
 * @tparam E element type this queue contains
 */
trait Queue[E] {
  /**
   * Enqueue an element
   *
   * @param element Element to be queued
   * @return Unique identifier for the queued element
   * @throws IOException when queueing fails
   */
  def enqueue(element: E): String
  /**
   * Remove and return the first element in the queue
   * @return None if queue is empty, the next element if not empty
   * @throws IOException when dequeueing fails
   */
  def dequeue(): Option[E]
}