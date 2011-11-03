import AssemblyKeys._ // put this at the top of the file

name := "TextGrounder"

version := "0.3.0"

organization := "OpenNLP"

scalaVersion := "2.9.1"

crossPaths := false

retrieveManaged := true

resolvers ++= Seq(
  "OpenNLP Maven Repository" at "http://opennlp.sourceforge.net/maven2"
//  Resolver for trove-scala source; nonexistent here yet
//  "repo.codahale.com" at "http://repo.codahale.com",
//  Resolver if you want to find stuff out of your local Maven cache
//  "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
  )


libraryDependencies ++= Seq(
  "com.google.inject" % "guice" % "2.0",
  "com.google.guava" % "guava" % "r06",
  "commons-cli" % "commons-cli" % "1.2",
  "org.jdom" % "jdom" % "1.1",
  "org.xerial" % "sqlite-jdbc" % "3.6.20",
  "org.apache.opennlp" % "opennlp-maxent" % "3.0.1-incubating",
  "org.apache.opennlp" % "opennlp-tools" % "1.5.1-incubating",
  // The use of %% instead of % causes the Scala version to get appended,
  // i.e. it's equivalent to the use of single % with "argot_2.9.1".
  // This is for Scala-specific dependencies.
  "org.clapper" %% "argot" % "0.3.5",
  "org.apache.hadoop" % "hadoop-core" % "0.20.205.0"
//  Find repository for trove-scala; currently stored unmanaged
//  "com.codahale" % "trove-scala_2.9.1" % "0.0.1-SNAPSHOT"
  )

// turn on all warnings in Java code
javacOptions ++= Seq("-Xlint")

// turn on all Scala warnings; also turn on deprecation warnings
scalacOptions ++= Seq("-deprecation", "-Xlint")

seq(assemblySettings: _*)

test in assembly := {}

jarName in assembly := "textgrounder-assembly.jar"
