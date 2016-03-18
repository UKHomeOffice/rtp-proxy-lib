import sbt.Keys._
import sbt._
import spray.revolver.RevolverPlugin._

object Build extends Build {
  val moduleName = "rtp-proxy-lib"

  val root = Project(id = moduleName, base = file("."))
    .configs(IntegrationTest)
    .settings(Revolver.settings)
    .settings(Defaults.itSettings: _*)
    .settings(
      name := moduleName,
      organization := "uk.gov.homeoffice",
      version := "1.3.1",
      scalaVersion := "2.11.8",
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
        "Artifactory Release Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-release-local/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
        "Kamon Repository" at "http://repo.kamon.io"
      )
    )
    .settings(libraryDependencies ++= {
      val `rtp-io-lib-version` = "1.7.1"
      val `rtp-test-lib-version` = "1.2.1"
      val `rtp-akka-lib-version` = "1.6.1"

      Seq(
        "org.scalactic" %% "scalactic" % "2.2.4" withSources(),
        "org.springframework" % "spring-core" % "4.2.1.RELEASE" withSources(),
        "uk.gov.homeoffice" %% "rtp-io-lib" % `rtp-io-lib-version` withSources(),
        "uk.gov.homeoffice" %% "rtp-test-lib" % `rtp-test-lib-version` withSources(),
        "uk.gov.homeoffice" %% "rtp-akka-lib" % `rtp-akka-lib-version` withSources()
      ) ++ Seq(
        "com.typesafe.akka" %% "akka-testkit" % "2.4.0" % Test withSources(),
        "io.spray" %% "spray-testkit" % "1.3.3" % Test withSources() excludeAll ExclusionRule(organization = "org.specs2"),
        "uk.gov.homeoffice" %% "rtp-io-lib" % `rtp-io-lib-version` % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-test-lib" % `rtp-test-lib-version` % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-akka-lib" % `rtp-akka-lib-version` % Test classifier "tests" withSources() excludeAll ExclusionRule(organization = "org.specs2")
      )
    })
}