import sbt._

object Deps {
  object Versions {
    val http4s = "0.21.22"
    val circe = "0.13.0"
    val doobie = "0.12.1"
    val scalatest = "3.2.2"
    val testContainers = "0.39.3"
    val scalamock = "5.1.0"
    val logback = "1.2.3"
    val tapir = "0.18.0-M7"
  }

  val http4s = Seq(
    "org.http4s" %% "http4s-core"         % Versions.http4s,
    "org.http4s" %% "http4s-server"       % Versions.http4s,
    "org.http4s" %% "http4s-dsl"          % Versions.http4s,
    "org.http4s" %% "http4s-blaze-server" % Versions.http4s,
    "org.http4s" %% "http4s-circe"        % Versions.http4s,
  )

  val circe = Seq(
    "io.circe" %% "circe-core"    % Versions.circe,
    "io.circe" %% "circe-generic" % Versions.circe,
    "io.circe" %% "circe-literal" % Versions.circe,
  )

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core"     % Versions.doobie,
    "org.tpolecat" %% "doobie-postgres" % Versions.doobie,
    "org.tpolecat" %% "doobie-hikari"   % Versions.doobie,
  )

  val logging = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback,
  )

  val scalaTest = Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest,
  )

  val scalaMock = Seq(
    "org.scalamock" %% "scalamock" % Versions.scalamock,
  )

  val testContainers = Seq(
    "com.dimafeng" %% "testcontainers-scala-scalatest"  % Versions.testContainers,
    "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testContainers,
  )

  val tapir = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s"  % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-core"               % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"      % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe"         % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"       % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % Versions.tapir,
  )
}