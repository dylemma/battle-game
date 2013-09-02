package io.dylemma.battle.example

import io.dylemma.battle._
import ResourceKey._
import SkillCostDSL._
import Combattant._

object SkillCostTest {

	def main(args: Array[String]): Unit = {

		val hp10 = cost(10, HP)
		val mana10 = cost(10, Mana)

		val hp5pMax = cost(5 % max(HP))
		val hp10pCurrent = cost(10 % current(HP))
		val mana5pMax = cost(5 % max(Mana))
		val mana10pCurrent = cost(10 % current(Mana))

		val a = new Combattant(HP -> 100, Mana -> 50)
		val aCost = cost(10, HP) + cost(10 % max(Mana))
		println(aCost)

		assert(aCost.canSpend(a))
		println("Before spending: " + a.resources)
		aCost.spend(a)
		println("After spending: " + a.resources)

		assert(cost(45, Mana) canSpend a)
		assert(!cost(46, Mana).canSpend(a))

		val b = new Combattant(Mana -> 100)

		val bCost = cost(50 % current(Mana)) + cost(10, Mana)
		println(bCost)
		assert(bCost canSpend b)
		bCost spend b
		println(b.resources)

		val c = new Combattant(Mana -> 100)
		val cCost = cost(10, Mana) + cost(50 % current(Mana))
		println(cCost)
		assert(cCost canSpend c)
		cCost spend c
		println(c.resources)

	}

}