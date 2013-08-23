package io.dylemma.battle.old

import scala.util.Random
import ResourceKey._
import SkillCostDSL._
import StatKey._

class Combattant extends HasResources with HasStats {
	val hp = new Resource(100)
	val mana = new Resource(50)

	def resources = {
		case HP => hp
		case Mana => mana
	}

	val str = new Stat(10)
	val agi = new Stat(15)
	val int = new Stat(11)

	def stats = {
		case Strength => str
		case Agility => agi
		case Intelligence => int
	}
}

sealed trait SkillAffiliation
case object Friendly extends SkillAffiliation
case object Neutral extends SkillAffiliation
case object Hostile extends SkillAffiliation

case class AppliedBattleEffect(effect: BattleEffect, target: Combattant, message: String)

trait BattleSkill {
	def cost: SkillCost
	def act(user: Combattant, target: Combattant): List[AppliedBattleEffect]
	def affiliation: SkillAffiliation
}

trait FriendlyBattleSkill extends BattleSkill { val affiliation = Friendly }
trait NeutralBattleSkill extends BattleSkill { val affiliation = Neutral }
trait HostileBattleSkill extends BattleSkill { val affiliation = Hostile }

object AttackSkill extends HostileBattleSkill {
	def cost = FreeCost
	def act(user: Combattant, target: Combattant) = {
		val damage = Random.nextInt(20) + 10

		AppliedBattleEffect(DamageEffect(damage), target, s"$user attacked $target for $damage damage!") :: Nil
	}
}

object HealSkill extends FriendlyBattleSkill {
	def cost = Mana cost 20
	def act(user: Combattant, target: Combattant) = {
		val healing = Random.nextInt(30) + 20
		AppliedBattleEffect(HealingEffect(healing), target, s"$user healed $target for $healing HP!") :: Nil
	}
}

object MagicSkill extends HostileBattleSkill {
	def cost = Mana cost 15
	def act(user: Combattant, target: Combattant) = {
		val damage = Random.nextInt(40) + 10
		AppliedBattleEffect(DamageEffect(damage), target, s"$user used magic on $target for $damage damage!") :: Nil
	}
}

class RandomizedAI {

	val skills: List[BattleSkill] = List(AttackSkill, HealSkill, MagicSkill)

	def takeTurn(me: Combattant, opponent: Combattant): Unit = {
		val availableSkills = skills filter { _.cost canSpend me }
		val choice = Random.nextInt(availableSkills.size)
		val skill = availableSkills(choice)
		skill.cost.spend(me)
		val appliedEffects = skill.affiliation match {
			case Friendly => skill.act(me, me)
			case _ => skill.act(me, opponent)
		}
		for (e <- appliedEffects) {
			e.effect affect e.target
			println(e.message)
		}
	}
}

object RandomBattle {
	def main(args: Array[String]): Unit = {
		object goodGuy extends Combattant {
			override def toString = "Good Guy"
		}
		object badGuy extends Combattant {
			override def toString = "Bad Guy"
		}

		def isDead(guy: Combattant): Boolean = {
			guy.hp.isEmpty
		}

		def status(guy: Combattant): String = {
			s"$guy: HP ${guy.hp.current}/${guy.hp.max}, MANA ${guy.mana.current}/${guy.mana.max}"
		}

		val AI = new RandomizedAI

		val alternate = Iterator.iterate(true) { b =>
			if (b) AI.takeTurn(goodGuy, badGuy)
			else AI.takeTurn(badGuy, goodGuy)
			!b
		}

		while (!isDead(goodGuy) && !isDead(badGuy)) {
			alternate.next()
			println(s"\t${status(goodGuy)}")
			println(s"\t${status(badGuy)}")
		}

		if (isDead(goodGuy)) {
			println(s"$badGuy defeated $goodGuy!")
		} else {
			println(s"$goodGuy defeated $badGuy!")
		}
	}
}