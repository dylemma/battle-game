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
		case SkillUsed(skill, user, target) => skill match {
			case Slash => AboutToDamageResource(target, HP, Damage(10, Slashing), Hostile)
			case Stab => AboutToDamageResource(target, HP, Damage(10, Piercing), Hostile)
			case Smash => AboutToDamageResource(target, HP, Damage(10, Blunt), Hostile)
			case _ => ()
		}
	}
}