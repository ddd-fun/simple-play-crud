name := "SimplePlayCrud"

version := "1.0"

lazy val `simpleplaycrud` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test,
                          "org.scalacheck" %% "scalacheck" % "1.13.0" % "test",
                          "com.amazonaws"  %  "aws-java-sdk-dynamodb" %  "1.11.15")


unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

routesImport += "play.api.mvc.PathBindable.bindableUUID"

DynamoDBLocal.settings

DynamoDBLocal.Keys.dynamoDBLocalDownloadDirectory := file("dynamodb-local")

test in Test <<= (test in Test).dependsOn(DynamoDBLocal.Keys.startDynamoDBLocal)

DynamoDBLocal.Keys.dynamoDBLocalInMemory := true

DynamoDBLocal.Keys.stopDynamoDBLocalAfterTests := true

test in Test <<= (test in Test).dependsOn(DynamoDBLocal.Keys.startDynamoDBLocal)
