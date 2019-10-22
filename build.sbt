import sbt.Keys._

lazy val commonSettings = Seq(
  publishArtifact in(Compile, packageDoc) := false,
  publishArtifact in packageDoc := false,
  sources in(Compile, doc) := Seq.empty,
  // disable using the Scala version in output paths and artifacts
  crossPaths := false,
  // removes Scala dependency
  autoScalaLibrary := false,

  resolvers ++= Seq(
    "releases" at "http://oss.sonatype.org/content/repositories/releases",
    "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
  ),
  publishArtifact in Test := false,
  publishMavenStyle := false,
  bintrayOrganization := Some("jetbrains"),
  bintrayRepository := "scalatest"
  //resolvers += Resolver.bintrayRepo("jetbrains", "scalatest"),
)


lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "scalatest-finders-patched",
    organization := "org.scalatest",
    version := "0.9.10",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    javacOptions in Global ++= Seq("-source", "1.8", "-target", "1.8")
  )

lazy val tests_2_1_7 = (project in file("tests_2_1_7"))
  .dependsOn(root)
  .settings(
    commonSettings,
    scalaVersion := "2.10.6",
    scalacOptions in Global ++= Seq("-deprecation"),
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.7" % Test
  )

lazy val tests_3_0_8 = (project in file("tests_3_0_8"))
  .dependsOn(root)
  .settings(
    commonSettings,
    scalaVersion := "2.12.9",
    scalacOptions in Global ++= Seq("-deprecation"),
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
  )
