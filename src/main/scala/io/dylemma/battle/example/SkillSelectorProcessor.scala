package io.dylemma.battle.example

import io.dylemma.battle._
import ResourceKey._
import DamageType._
import Affiliation._
import Combattant._
import scala.concurrent.{ Await, ExecutionContext, Future }
import ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SkillSelectorProcessor(skills: List[Skill], user: Combattant, defaultTarget: Target) extends EventProcessor {
	def priority = 10
	private val skillMap = skills.zipWithIndex.map { case (s, i) => (i, s) }.toMap

	def process(implicit exc: ExecutionContext) = {
		case TurnBegan => {
			def getUserChoice: Skill = {
				println("Type a number to select a skill:")
				for { (s, i) <- skills.zipWithIndex } println(s"  $i: $s")
				try {
					val i = readInt
					skillMap get i getOrElse {
						println("invalid response")
						getUserChoice
					}
				} catch {
					case t: Throwable =>
						println("invalid response")
						getUserChoice
				}
			}
			val futureSkill = Future { getUserChoice }
			for (skill <- futureSkill) yield SkillUsed(skill, user, defaultTarget)
		}
	}
}

object SkillSelectorProcessor {

	val hero = new Combattant() { override def toString = "Hero" }
	val skills = {
		import Skills._
		List(Slash, Smash, Stab)
	}

	def main(args: Array[String]): Unit = {
		val target = new CombattantTarget(new Combattant(HP -> 20) { override def toString = "Villain" })
		val processor = new SkillSelectorProcessor(skills, hero, target)
		val q = new EventQueue(List(processor, SkillProcessor, new ResourceModificationProcessor))

		val end = q.runEventQueue(List(TurnBegan, TurnEnded, TurnBegan, TurnEnded))
		Await.ready(end, 60.seconds)
		println("(done)")
	}
}