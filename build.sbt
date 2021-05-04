import Deps._

name := "Wishlist"

version := "0.1"

scalaVersion := "2.13.5"

ThisBuild / scalacOptions ++= Seq(
  "-Xlint:unused",
  "-Xfatal-warnings",
  "-deprecation",
)

libraryDependencies ++= (http4s ++ tapir ++ circe ++ doobie ++ logging ++ (scalaTest ++ scalaMock ++ testContainers)
  .map(_ % Test))


