name := "durable-data"

crossScalaVersions := Seq("2.10.4", "2.11.2")

version := "1.1"

organization := "com.lunatech"

libraryDependencies += "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3"

libraryDependencies += "org.specs2" %% "specs2-core" % "2.4.6" % "test"

publishTo in ThisBuild <<= version { (v: String) =>
  val path = if(v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
  Some(Resolver.url("Lunatech Artifactory", new URL("http://artifactory.lunatech.com/artifactory/%s/" format path)))
}

