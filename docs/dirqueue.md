
DirectoryBackedQueue
===

This is a filesystem based, persistent queue in Scala. It's intended to accommodate message passing between processes, without the receiving process to be running all the time.

There is a `Queue` interface trait, and a single implementation: `DirectoryBackedQueue`. The `Queue` interface has only two methods: `enqueue(elem: E): String` and `dequeue(): Option[E]`. The `dequeue` method returns a `None` if the queue is empty and a `Some` if there is an element.

All methods are thread safe. The main operational mode is with one reader and one or multiple writers.

Usage
-----

A `com.lunatech.queue.Serializable` typeclass intance is required for the element type that is queued. An instance for `String` is provided.

The backing directory must already exist.

Producer:

    val directory = new File("./my-backing-dir").toPath
    val queue = DirectoryBackedQueue[String](directory)
    queue.enqueue("foo")

Consumer:

    val directory = new File("./my-backing-dir").toPath
    val queue = DirectoryBackedQueue[String](directory)
    queue.deqeuue.map { element =>
      // We've got an element
    }

