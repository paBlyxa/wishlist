import Deps._

name := "Wishlist"

version := "0.1"

scalaVersion := "2.13.5"

ThisBuild / scalacOptions ++= Seq(
  "-Xlint:unused",
  "-Xfatal-warnings",
  "-deprecation",
  "-Ymacro-annotations",
)

libraryDependencies ++= (http4s ++ tapir ++ circe ++ doobie ++ logging ++ (scalaTest ++ scalaMock ++ testContainers)
  .map(_ % Test)) ++ enumeratum ++ newtype

enablePlugins(JavaAppPackaging, DockerPlugin)

dockerBaseImage := "openjdk:11-jre"
dockerExposedPorts := Seq(8080)
