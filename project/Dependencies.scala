import sbt._
import sbt.Keys._

object Dependencies {

  val catsVersion       = "2.0.0"
  val catsEffectVersion = "2.0.0"
  val scalaCheckVersion = "1.14.0"
  val specs2Version     = "4.6.0"
  val http4sVersion     = "0.20.0"
  val log4catsVersion   = "1.0.0-RC1"
  val logbackVersion    = "1.2.3"
  val akkaHttpVersion   = "10.1.9"
  val akkaStreamVersion = "2.5.23"

  val akkaLib = Seq("com.typesafe.akka" %% "akka-http"   % akkaHttpVersion, 
                    "com.typesafe.akka" %% "akka-stream" % akkaStreamVersion)
  val catsLib       = "org.typelevel" %% "cats-core" % catsVersion
  val catsFreeLib   = "org.typelevel" %% "cats-free" % catsVersion
  val catsEffLib    = "org.typelevel" %% "cats-effect" % catsEffectVersion
  val scalaCheckLib = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  val specs2Lib     = "org.specs2" %% "specs2-core" % specs2Version
  val specs2SCLib   = "org.specs2" %% "specs2-scalacheck" % specs2Version
  val http4sLib     = Seq("org.http4s" %% "http4s-dsl" % http4sVersion,
                          "org.http4s" %% "http4s-blaze-server" % http4sVersion,
                          "org.http4s" %% "http4s-blaze-client" % http4sVersion)
  val logbackLib    = "ch.qos.logback" % "logback-classic" % logbackVersion
  val log4catsLib   = Seq(
    "io.chrisdavenport" %% "log4cats-core"    % log4catsVersion,
    "io.chrisdavenport" %% "log4cats-slf4j"   % log4catsVersion 
  )
  val myDependencies = Seq(
    catsLib,
    catsFreeLib,
    catsEffLib,
    logbackLib
  ) ++ http4sLib ++ log4catsLib ++ akkaLib

}


