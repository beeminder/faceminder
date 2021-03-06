name := "faceminder"

version := "1.0-SNAPSHOT"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
    jdbc,
    anorm,
    cache,
    "com.typesafe.play" %% "play-slick" % "0.6.0.1",
    "org.xerial" % "sqlite-jdbc" % "3.7.2",
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "org.scala-tools.time" % "time_2.9.1" % "0.5",
    "com.github.nscala-time" %% "nscala-time" % "1.4.0",
    "com.jolbox" % "bonecp" % "0.8.0.RELEASE",
    "org.reflections" % "reflections" % "0.9.8"
)

play.Project.playScalaSettings

org.scalastyle.sbt.ScalastylePlugin.Settings

// Create a default Scala style task to run with tests
lazy val testScalaStyle = taskKey[Unit]("testScalaStyle")

testScalaStyle := {
    org.scalastyle.sbt.PluginKeys.scalastyle.toTask("").value
}

(test in Test) <<= (test in Test) dependsOn testScalaStyle

