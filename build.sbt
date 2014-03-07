name := "dirqueue"

scalaVersion := "2.10.2"

version := "0.1"

organization := "com.lunatech"

libraryDependencies += "com.fasterxml.uuid" % "java-uuid-generator" % "3.1.3"

libraryDependencies += "org.specs2" %% "specs2" % "2.2" % "test"

publishTo in ThisBuild <<= version { (v: String) =>
  val path = if(v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
  Some(Resolver.url("Lunatech Artifactory", new URL("http://artifactory.lunatech.com/artifactory/%s/" format path)))
}

