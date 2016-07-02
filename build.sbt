name := "SimplePlayCrud"

version := "1.0"

lazy val `simpleplaycrud` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test, "org.scalacheck" %% "scalacheck" % "1.13.0" % "test" )


unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"  