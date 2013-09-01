package io.dylemma.battle.example

import io.dylemma.battle._
import ResourceKey._
import DamageType._
import Affiliation._
import Combattant._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await

object Damaging {

	val hero = new Combattant(HP -> 30) {
		override def toString = "Hero"
	}

	def main(args: Array[String]): Unit = {
		logThreshold = Debug
		val processor = new ResourceModificationProcessor
		val q = new EventQueue(List(processor))
		val target = CombattantTarget(hero)

		val end = q.runEventQueue(List(
			AboutToDamageResource(target, HP, Damage(10, Fire), Hostile),
			AboutToDamageResource(target, HP, Damage(25, Slashing), Hostile)))
		Await.ready(end, 5.seconds)
	}
}