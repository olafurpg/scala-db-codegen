lazy val `launaskil-codegen` =
  (project in file(".")).settings(
    name := "codegen",
    scalaVersion := "2.11.8",
    libraryDependencies ++= Seq(
      "com.geirsson" %% "scalafmt-core" % "0.3.0",
      "io.getquill" %% "quill-core" % "0.8.0",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
      "com.github.alexarchambault" %% "case-app" % "1.1.0-RC3",
      "org.scalatest" %% "scalatest" % "3.0.0" % "test"
    )
  )
