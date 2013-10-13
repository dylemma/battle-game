package io.dylemma.battle.example

import io.dylemma.battle._
import ResourceKey._
import DamageType._
import Affiliation._
import Combattant._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import io.dylemma.util.BidiMap

object Damaging {

	val hero = new Combattant(HP -> 30) {
		override def toString = "Hero"
	}

	def main(args: Array[String]): Unit = {
		logThreshold = Debug
		val processor = new ResourceModificationProcessor
		val q = new EventProcessor(Set(processor), Battleground(BidiMap(), BattleModifiers.empty))

		q
			.process(DamageResource(hero, HP, Damage(10, Fire)))
			.process(DamageResource(hero, HP, Damage(25, Slashing)))

	}
}