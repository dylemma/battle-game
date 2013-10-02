package io.dylemma.battle

import Skills._
import StatKey._
import ResourceKey._
import DamageType._
import Affiliation._
import DamageFormula._
import Damage._

trait Skill {
	def calculatePriority(user: Combattant, target: Target, mods: BattleModifiers): Priority
	def activate(user: Combattant, target: Target, mods: BattleModifiers): List[Event]
}

trait UnprioritizedSkill extends Skill {
	def calculatePriority(user: Combattant, target: Target, mods: BattleModifiers) = Priority(0)
}

object Skills extends TargetHelpers {

	case object Slash extends Skill with UnprioritizedSkill {

		val calcDamage = basicDamage(10, Strength, Strength) ~ Slashing ~ attackerSource ~ randomChanceMultiplier ~ criticalMultiplier(0.1, 2)

		def activate(user: Combattant, target: Target, mods: BattleModifiers) = {
			target.projectAs[HasResources].toList map { t =>
				DamageResource(t, HP, calcDamage(user, target, mods))
			}
		}
	}

	case object Stab extends Skill with UnprioritizedSkill {

		val calcDamage = basicDamage(10, Agility, Strength) ~ Piercing ~ attackerSource ~ randomChanceMultiplier ~ criticalMultiplier(0.2, 2)

		def activate(user: Combattant, target: Target, mods: BattleModifiers) = {
			target.projectAs[HasResources].toList map { t =>
				DamageResource(t, HP, calcDamage(user, target, mods))
			}
		}
	}

	case object Smash extends Skill with UnprioritizedSkill {

		val calcDamage = basicDamage(15, Strength, Strength) ~ Blunt ~ attackerSource ~ randomChanceMultiplier ~ criticalMultiplier(0.05, 2)

		def activate(user: Combattant, target: Target, mods: BattleModifiers) = {
			target.projectAs[HasResources].toList map { t =>
				DamageResource(t, HP, calcDamage(user, target, mods))
			}
		}
	}
}

object SkillProcessor extends EventHandler {
	def priority = Priority(1)

	def handlePreEvent(mods: BattleModifiers) = PartialFunction.empty

	def handlePostEvent(mods: BattleModifiers) = {
		case CombattantAction(user, SkillUse(skill, target)) =>
			skill.activate(user, target, mods) map NewEvent
	}
}