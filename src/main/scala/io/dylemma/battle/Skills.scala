package io.dylemma.battle

//import Skills._
import StatKey._
import ResourceKey._
import DamageType._
import Affiliation._
import DamageFormula._
import Damage._
import EventHandlerSyntax._
import scala.concurrent.ExecutionContext

trait Skill {
	def calculatePriority(user: Combattant, target: Target, context: BattleContext): Priority
	def activate(user: Combattant, target: Target, context: BattleContext): List[Event]
	def targetMode: TargetMode
}

trait UnprioritizedSkill extends Skill {
	def calculatePriority(user: Combattant, target: Target, context: BattleContext) = Priority(0)
}

object Skills extends TargetHelpers {

	case object Slash extends Skill with UnprioritizedSkill {

		val calcDamage = basicDamage(10, Strength, Strength) ~ Slashing ~ attackerSource ~ randomChanceMultiplier ~ criticalMultiplier(0.1, 2)

		def activate(user: Combattant, target: Target, context: BattleContext) = {
			target.projectAs[HasResources].toList map { t =>
				DamageResource(t, HP, calcDamage(user, target, context))
			}
		}

		val targetMode = TargetMode.OpposingParty
	}

	case object Stab extends Skill with UnprioritizedSkill {

		val calcDamage = basicDamage(10, Agility, Strength) ~ Piercing ~ attackerSource ~ randomChanceMultiplier ~ criticalMultiplier(0.2, 2)

		def activate(user: Combattant, target: Target, context: BattleContext) = {
			target.projectAs[HasResources].toList map { t =>
				DamageResource(t, HP, calcDamage(user, target, context))
			}
		}

		val targetMode = TargetMode.OpposingParty
	}

	case object Smash extends Skill with UnprioritizedSkill {

		val calcDamage = basicDamage(15, Strength, Strength) ~ Blunt ~ attackerSource ~ randomChanceMultiplier ~ criticalMultiplier(0.05, 2)

		def activate(user: Combattant, target: Target, context: BattleContext) = {
			target.projectAs[HasResources].toList map { t =>
				DamageResource(t, HP, calcDamage(user, target, context))
			}
		}

		val targetMode = TargetMode.OpposingParty
	}

	case object QuarterHeal extends Skill with UnprioritizedSkill {
		val targetMode = TargetMode.SameParty
		def activate(user: Combattant, target: Target, context: BattleContext) = {
			val restoreOpt = for {
				ally <- target.projectAs[HasResources]
			} yield {
				val maxHp = ally.getResource(HP).max
				RestoreResource(ally, HP, maxHp / 4)
			}
			restoreOpt.toList
		}
	}
}

object SkillProcessor extends SyncEventHandler {
	def priority = Priority(1)

	def handlePreEvent(context: BattleContext) = PartialFunction.empty

	def handlePostEvent(context: BattleContext) = {
		case CombattantAction(user, SkillUse(skill, target)) => reactions {
			skill.activate(user, target, context)
		}
	}
}