name := "booklikst"

version := "0.1"

scalaVersion := "2.13.11"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-core" % "22.12.0",
  "com.twitter" %% "finagle-http" % "22.12.0",
  "com.github.ben-manes.caffeine" % "caffeine" % "2.9.3",
  "org.scalatest" %% "scalatest" % "3.2.10" % "test",
  "org.scalamock" %% "scalamock" % "5.1.0" % "test",
)
