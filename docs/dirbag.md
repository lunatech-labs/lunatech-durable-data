
DirectoryBackedBag
===

This is a filesystem based, durable bag in Scala. It's intended to keep a record of running tasks in a system, allowing them to be recovered on restart after application failure.

There is a `Bag` interface trait, and a single implementation: `DirectoryBackedBag`. The `Bag` interface has only three methods: `insert(elem: E): String`, `remove(key: String): Unit` and `list: Map[String, E]`.

Usage
-----

A `com.lunatech.queue.Serializable` typeclass intance is required for the element type that is queued. An instance for `String` is provided.

The backing directory must already exist.

Usage example:

    val directory = new File("./my-backing-dir").toPath
    val queue = DirectoryBackedBag[String](directory)    
    val key = bag.insert("foo")
    
    // Later:
    bag.remove(key)
    
    // On application start:
    bag.list.foreach { case (_, value) => println(s"Task ${value} was not completed when the app was killed!") }


