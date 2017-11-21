lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishMavenStyle := true,
  publishArtifact := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq(
    "MIT" -> url("http://www.opensource.org/licenses/mit-license.php")),
  homepage := Some(url("https://github.com/olafurpg/db-codegen")),
  autoAPIMappings := true,
  apiURL := Some(url("https://github.com/olafurpg/db-codegen")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/olafurpg/db-codegen"),
      "scm:git:git@github.com:olafurpg/db-codegen.git"
    )
  ),
  pomExtra :=
    <developers>
        <developer>
          <id>olafurpg</id>
          <name>Ólafur Páll Geirsson</name>
          <url>https://geirsson.com</url>
        </developer>
      </developers>
)

enablePlugins(PackPlugin)
import xerial.sbt.pack.PackPlugin.packSettings

lazy val `scala-db-codegen` =
  (project in file("."))
    .settings(packSettings)
    .settings(publishSettings)
    .settings(
      name := "scala-db-codegen",
      organization := "com.geirsson",
      scalaVersion := "2.12.4",
      version := com.geirsson.codegen.Versions.nightly,
      packMain := Map("scala-db-codegen" -> "com.geirsson.codegen.Codegen"),
      libraryDependencies ++= Seq(
        "com.geirsson" %% "scalafmt-core" % "1.2.0",
        "io.getquill" %% "quill-core" % "2.2.0",
        "com.h2database" % "h2" % "1.4.196",
        "org.postgresql" % "postgresql" % "42.1.4",
        "com.github.alexarchambault" %% "case-app" % "1.2.0",
        "org.scalatest" %% "scalatest" % "3.0.4" % "test"
      )
    )
