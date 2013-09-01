package io.dylemma.battle

import Skills._
import StatKey._
import ResourceKey._
import DamageType._
import Affiliation._
import scala.concurrent.ExecutionContext

trait Skill

object Skills {
	case object Slash extends Skill
	case object Stab extends Skill
	case object Smash extends Skill
}

object SkillProcessor extends EventProcessor {
	def priority = 1

	def process(implicit exc: ExecutionContext) = {

		// Slash deals hostile slashing damage to the target's HP
		case SkillUsed(Slash, user, target) =>
			AboutToDamageResource(target, HP, Damage(10, Slashing), Hostile)

		// Stab deals hostile piercing damage to the target's HP
		case SkillUsed(Stab, user, target) =>
			AboutToDamageResource(target, HP, Damage(10, Piercing), Hostile)

		// Smash deals hostile blunt damage to the target's HP
		case SkillUsed(Smash, user, target) =>
			AboutToDamageResource(target, HP, Damage(10, Blunt), Hostile)
	}
}