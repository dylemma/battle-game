import sbt._
import Keys._

object BattlesBuild extends Build {

	val enableContinuations = Seq(
		libraryDependencies += compilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.3"),
		scalacOptions += "-P:continuations:enable"
	)

	val baseSettings = Seq(
		organization := "io.dylemma",
		scalaVersion := "2.10.3",
		version := "0.1-SNAPSHOT"
	)

	object deps {
		val akka = "com.typesafe.akka" %% "akka-actor" % "2.2.3"
		val scalatest = "org.scalatest" %% "scalatest" % "1.9.1"
		val scalaAsync = "org.scala-lang.modules" %% "scala-async" % "0.9.0-M2"
		val sprayCan = "io.spray" % "spray-can" % "1.2-RC2"
		val sprayJson = "io.spray" %% "spray-json" % "1.2.5"
		
		val sprayResolver = "spray repo" at "http://repo.spray.io"
	}
	
	val addResolvers = resolvers ++= Seq(
		deps.sprayResolver
	)
	
	val setDependencies = libraryDependencies ++= Seq(
		deps.akka,
		deps.sprayCan,
		deps.sprayJson,
		deps.scalaAsync,
		deps.scalatest % "test"
	)
	
	lazy val root = Project("BattleV2", file("."))
		.settings(baseSettings: _*)
		.settings(enableContinuations: _*)
		.settings(addResolvers)
		.settings(setDependencies)

}