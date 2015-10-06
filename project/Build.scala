import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

object Build extends Build {
  val moduleName = "rtp-proxy-service"

  lazy val proxy = Project(id = moduleName, base = file("."))
    .configs(IntegrationTest)
    .settings(Revolver.settings)
    .settings(Defaults.itSettings: _*)
    .settings(
      name := moduleName,
      organization := "uk.gov.homeoffice",
      version := "1.0-SNAPSHOT",
      scalaVersion := "2.11.7",
      scalacOptions ++= Seq(
        "-feature",
        "-language:implicitConversions",
        "-language:higherKinds",
        "-language:existentials",
        "-language:reflectiveCalls",
        "-language:postfixOps",
        "-Yrangepos",
        "-Yrepl-sync"),
      ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) },
      resolvers ++= Seq(
        "Artifactory Snapshot Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
        "Kamon Repository" at "http://repo.kamon.io"),
      libraryDependencies ++= Seq(
        "org.clapper" %% "grizzled-slf4j" % "1.0.2",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "com.typesafe" % "config" % "1.3.0" withSources(),
        "org.json4s" %% "json4s-jackson" % "3.2.11" withSources(),
        "org.json4s" %% "json4s-ext" % "3.2.11" withSources(),
        "org.scalactic" %% "scalactic" % "2.2.4" withSources(),
        "org.springframework" % "spring-core" % "4.2.1.RELEASE" withSources()),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-testkit" % "2.3.12" % Test withSources(),
        "io.spray" %% "spray-testkit" % "1.3.3" % Test withSources() excludeAll ExclusionRule(organization = "org.specs2"),
        "com.github.fakemongo" % "fongo" % "1.6.2" % Test withSources(),
        "uk.gov.homeoffice" %% "rtp-test-lib" % "1.0-SNAPSHOT" % Test classifier "tests" withSources()
      ))

  val ioPath = "../rtp-io-lib"
  val akkaPath = "../rtp-akka-lib"
  val testPath = "../rtp-test-lib"

  val root = if (file(ioPath).exists && sys.props.get("jenkins").isEmpty) {
    println("=============")
    println("Build Locally")
    println("=============")

    val io = ProjectRef(file(ioPath), "rtp-io-lib")
    val akka = ProjectRef(file(akkaPath), "rtp-akka-lib")
    val test = ProjectRef(file(testPath), "rtp-test-lib")

    proxy.dependsOn(io % "test->test;compile->compile")
         .dependsOn(akka % "test->test;compile->compile")
         .dependsOn(test % "test->test;compile->compile")
  } else {
    println("================")
    println("Build on Jenkins")
    println("================")

    proxy.settings(
      libraryDependencies ++= Seq(
        "uk.gov.homeoffice" %% "rtp-io-lib" % "1.0-SNAPSHOT" withSources(),
        "uk.gov.homeoffice" %% "rtp-io-lib" % "1.0-SNAPSHOT" % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-akka-lib" % "1.0-SNAPSHOT" withSources(),
        "uk.gov.homeoffice" %% "rtp-akka-lib" % "1.0-SNAPSHOT" % Test classifier "tests" withSources() excludeAll ExclusionRule(organization = "org.specs2"),
        "uk.gov.homeoffice" %% "rtp-test-lib" % "1.0-SNAPSHOT" withSources(),
        "uk.gov.homeoffice" %% "rtp-test-lib" % "1.0-SNAPSHOT" % Test classifier "tests" withSources()
      )
    )
  }
}