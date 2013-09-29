package io.dylemma.battle

import Skills._
import StatKey._
import ResourceKey._
import DamageType._
import Affiliation._
import scala.concurrent.ExecutionContext

trait Skill {
	def calculatePriority(user: Combattant, target: Target, mods: BattleModifiers): Priority
	def activate(user: Combattant, target: Target, mods: BattleModifiers): List[Event]
}

trait UnprioritizedSkill extends Skill {
	def calculatePriority(user: Combattant, target: Target, mods: BattleModifiers) = Priority(0)
}

object Skills extends TargetHelpers {

	case object Slash extends Skill with UnprioritizedSkill {
		def activate(user: Combattant, target: Target, mods: BattleModifiers) = {
			target.project[HasResources].toList map { t =>
				DamageResource(t, HP, Damage(10, Slashing))
			}
		}
	}

	case object Stab extends Skill with UnprioritizedSkill {
		def activate(user: Combattant, target: Target, mods: BattleModifiers) = {
			target.project[HasResources].toList map { t =>
				DamageResource(t, HP, Damage(10, Piercing))
			}
		}
	}

	case object Smash extends Skill with UnprioritizedSkill {
		def activate(user: Combattant, target: Target, mods: BattleModifiers) = {
			target.project[HasResources].toList map { t =>
				DamageResource(t, HP, Damage(10, Blunt))
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