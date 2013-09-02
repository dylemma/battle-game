package test.io.dylemma.battle

import io.dylemma.battle._
import SkillCostDSL._
import ResourceKey._
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter

class SkillCostSuite extends FunSuite {

	def newGuy = new Combattant(HP -> 100, Mana -> 100)

	test("test subject combattant has the right resources") {
		val guy = newGuy
		val guyHP = guy getResource HP
		val guyMana = guy getResource Mana
		assert(guyHP.current == 100)
		assert(guyHP.max == 100)
		assert(guyMana.current == 100)
		assert(guyMana.max == 100)
	}

	test("flat skill cost correctly returns true for `canSpend` when cost is equal to current resource") {
		val guy = newGuy
		val c = cost(100, Mana)
		assert(c canSpend guy)
	}

	test("flat skill cost does not modify target's resources while checking `canSpend`") {
		val guy = newGuy
		val c = cost(90, Mana)
		c canSpend guy
		val guyHP = guy getResource HP
		val guyMana = guy getResource Mana
		assert(guyHP.current == 100 && guyMana.current == 100)
	}

	test("flat skill cost correctly returns false for `canSpend` when the cost is greater than the current resource") {
		val guy = newGuy
		guy.getResource(Mana).deplete(50)
		val c = cost(60, Mana)
		assert(!c.canSpend(guy))
	}

	test("flat skill cost returns false when the target doesn't have the right resource") {
		val guy = new Combattant(HP -> 100)
		val c = cost(10, Mana)
		assert(!c.canSpend(guy))
	}

	test("flat skill cost correctly depletes resources in `spend`") {
		val guy = newGuy
		val c = cost(10, Mana)
		c.spend(guy)
		val guyMana = guy.getResource(Mana)
		assert(guyMana.current == 90 && guyMana.max == 100)
	}

	test("percentage-max skill cost correctly returns true for `canSpend`") {
		val guy = newGuy
		val c = cost(50 % max(Mana))
		assert(c canSpend guy)
	}

	test("percentage-max skill cost does not modify target when checking `canSpend`") {
		val guy = newGuy
		val c = cost(50 % max(Mana))
		c canSpend guy
		val guyHP = guy getResource HP
		val guyMana = guy getResource Mana
		assert(guyHP.current == 100 && guyMana.current == 100)
	}

	test("percentage-max skill cost correctly returns false for `canSpend`") {
		val guy = newGuy
		guy getResource Mana deplete 90
		val c = cost(20 % max(Mana))
		assert(!c.canSpend(guy))
	}

	test("percentage-max skill cost correctly depletes resources in `spend`") {
		val guy = newGuy
		val c = cost(10 % max(HP))
		c spend guy
		val guyHP = guy getResource HP
		assert(guyHP.current == 90 && guyHP.max == 100)
	}

	test("percentage-max skill cost can be spent on a target that doesn't have the appropriate resource") {
		val guy = new Combattant(HP -> 100)
		val c = cost(10 % max(Mana))
		assert(c canSpend guy)
	}

	test("percentage-current skill cost correctly returns true for `canSpend`") {
		val guy = newGuy
		val c = cost(100 % current(Mana))
		assert(c canSpend guy)
	}

	test("percentage-current skill cost correctly depletes resources in `spend`") {
		val guy = newGuy
		val guyMana = guy getResource Mana
		guyMana deplete 50

		val c = cost(50 % current(Mana))
		c spend guy
		assert(guyMana.current == 25)
	}

	test("percentage-current skill cost does not modify target when checking `canSpend`") {
		val guy = newGuy
		val c = cost(50 % current(Mana))
		c canSpend guy
		val guyHP = guy getResource HP
		val guyMana = guy getResource Mana
		assert(guyHP.current == 100 && guyMana.current == 100)
	}

	test("percentage-current skill cost can be spent on a target that doesn't have the appropriate resource") {
		val guy = new Combattant(HP -> 100)
		val c = cost(10 % current(Mana))
		assert(c canSpend guy)
	}

	test("combined flat skill costs correctly return true for `canSpend`") {
		val guy = newGuy
		val c = cost(10, Mana) + cost(10, HP)
		assert(c canSpend guy)
	}

	test("combined flat skill costs return false for `canSpend` when the target doesn't have enough of one resource") {
		val guy = newGuy
		val c = cost(10, HP) + cost(110, Mana)
		assert(!c.canSpend(guy))
	}

	test("combined flat skill costs correctly deplete resources") {
		val guy = newGuy
		val c = cost(10, HP) + cost(20, Mana)
		c spend guy
		val guyHP = guy getResource HP
		val guyMana = guy getResource Mana
		assert(guyHP.current == 90 && guyMana.current == 80)
	}

	test("combined flat+percentageCurrent skill cost depletes the flat cost, then the current cost") {
		val guy = newGuy
		//c = 80 HP, and then 50% of the remaining 20, for a total of 90 HP spent
		val c = cost(80, HP) + cost(50 % current(HP))
		c spend guy
		val guyHP = guy getResource HP
		assert(guyHP.current == 10)
	}

	test("combined percentageCurrent+flat skill cost depletes the percentage, then the flat cost: `canSpend` returns false, does not modify target") {
		val guy = newGuy
		val c = cost(50 % current(Mana)) + cost(80, Mana)
		assert(!c.canSpend(guy))
		val guyMana = guy getResource Mana
		assert(guyMana.current == 100)
	}

	test("combined percentageMax correctly return false when the target doesn't have enough") {
		val guy = newGuy
		val c = cost(60 % max(HP)) + cost(60 % max(HP))
		assert(!c.canSpend(guy))
	}

	test("combined percentageCurrent skill costs correctly deplete resources accumulatively") {
		val guy = newGuy
		// c = 20 Mana + (50% of 80 = 40) Mana
		val c = cost(20 % current(Mana)) + cost(50 % current(Mana))
		c spend guy
		val guyMana = guy getResource Mana
		assert(guyMana.current == 40)
	}

}