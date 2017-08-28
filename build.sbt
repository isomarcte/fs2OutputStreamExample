lazy val fs2Version = "0.9.7"

lazy val commonSettings =
  Seq(
    organization := "xyz.isomarcte",
    version := "1.0.0-SNAPSHOT",
    scalaVersion := "2.12.3",
    scalacOptions ++= Seq(
      "-deprecation"
    ),
    crossScalaVersions := Seq(
      "2.11.11"
    )
  )

lazy val root =
  (
    project in file(".")
  ).settings(
    commonSettings
  ).aggregate(fs2JavaStream)

lazy val fs2JavaStream =
  project.settings(
    commonSettings,
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % fs2Version,
      "co.fs2" %% "fs2-io" % fs2Version
    )
  )
