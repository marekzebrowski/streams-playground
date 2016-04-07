name := "play-str"

scalaVersion := "2.11.8"

libraryDependencies ++={
 val  akkaVersion = "2.4.3"
 Seq("com.typesafe.akka" %% "akka-stream" % akkaVersion)
}
