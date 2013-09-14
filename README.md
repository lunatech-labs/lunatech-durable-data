dirqueue
===

Dirqueue is a filesystem based, persistent queue in Scala. It's intended to accommodate message passing between processes, without the receiving process to be running all the time.

There is a `Queue` interface trait, and a single implementation: `DirectoryBackedQueue`. The `Queue` interface has only two methods: `enqueue(elem: E): String` and `dequeue(): Option[E]`. The `dequeue` method returns a `None` if the queue is empty and a `Some` if there is an element.

usage
===

A `com.lunatech.queue.Serializable` typeclass intance is required for the element type that is queued. An instance for `String` is provided.

The backing directory must already exist.

Producer:

    val directory = File("./my-backing-dir").toPath
    val queue = DirectoryBackedQueue[String](directory)
    queue.enqueue("foo")

Consumer:

    val directory = File("./my-backing-dir").toPath
    val queue = DirectoryBackedQueue[String](directory)
    queue.deqeuue.map { element =>
      // We've got an element
    }

Development
===

 * Build with `sbt compile`
 * Generate Scaladoc with `sbt doc`
 * Run tests with `sbt test`

License
===
This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 Lunatech Labs (http://www.lunatech.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.