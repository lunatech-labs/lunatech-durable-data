Durable Data
===

Lunatech Durable is a project contains several very simple Scala data structures that are backed by a filesystem. These structures survive application restarts and because they are so simple and easy to fully understand their behaviour, they are useful building blocks.

Goals
---

To provide foundational durable data structures whose behaviour under all circumstances is easy to understand.

Datastructures
---

* [DirectoryBackedQueue](docs/dirqueue.md) is a durable queue, that can be used from multiple processes. It's aim is to provide inter-process communication.
* [DirectoryBackedBag](docs/dirbag.md) is a durable bag (multiset). It's main usecase is creating a durable copy of running tasks in a system, that can be used to restart the tasks if the system restarts.

Quick start
-----------

Durable Data is built against Scala 2.10 and 2.11.


Add to your `build.sbt`:

    resolvers += "Lunatech Public Releases" at "http://artifactory.lunatech.com/artifactory/releases-public"

    libraryDependencies += "com.lunatech" %% "dirqueue" % "0.1"
    
Then see the links in the `Datastructures` section of this README for additional documentation for each data structure.


Development
-----------

 * Build with `sbt compile`
 * Generate Scaladoc with `sbt doc`
 * Run tests with `sbt test`

License
-------
This software is licensed under the Apache 2 license, quoted below.

Copyright 2013 Lunatech Labs (http://www.lunatech.com).

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
