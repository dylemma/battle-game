package io.dylemma.battle.example

import io.dylemma.battle._
import ResourceKey._
import StatKey._
import DamageType._
import Affiliation._
import Combattant._
import scala.concurrent.{ Await, ExecutionContext, Future }
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import io.dylemma.util.BidiMap
import EventHandlerSyntax._
import scala.util.Failure
import scala.util.Success

object SkillSelectorProcessor {
	def main(args: Array[String]): Unit = {

		val hero = new Combattant(level(5), HP -> 40, Mana -> 15, Strength -> 10, Agility -> 10) { override def toString = "Hero" }
		val ally = new Combattant(level(5), HP -> 35, Mana -> 20, Strength -> 8, Agility -> 12) { override def toString = "Ally" }
		val enemy = new Combattant(level(6), HP -> 50, Mana -> 20, Strength -> 15, Agility -> 12) { override def toString = "Villain" }

		val skills: List[Skill] = {
			import Skills._
			List(Slash, Smash, Stab, QuarterHeal)
		}

		val allTargets: List[(Target, String)] = List(
			EnvironmentTarget -> "environment",
			CombattantTarget(hero) -> "hero",
			CombattantTarget(ally) -> "ally",
			CombattantTarget(enemy) -> "enemy")

		def printStatus = {
			def statusLine(cmb: Combattant) = {
				val hp = cmb.getResource(HP)
				val mana = cmb.getResource(Mana)
				s"${cmb.toString}: [HP ${hp.current}/${hp.max}] [Mana ${mana.current}/${mana.max}]"
			}
			val status = s"""My Party:
				|  ${statusLine(hero)}
				|  ${statusLine(ally)}
				|
				|Enemy Party:
				|  ${statusLine(enemy)}
				|""".stripMargin

			println(status)
		}

		def promptSkill: Skill = {
			val skillMsgs = skills.zipWithIndex.map {
				case (s, i) => s"[$i: $s]"
			}.mkString(" ")
			println("Pick a skill: " + skillMsgs)
			try {
				val i = readInt
				skills(i)
			} catch {
				case t: Throwable =>
					println("invalid response")
					promptSkill
			}
		}

		def promptTarget(skill: Skill, user: Combattant, battleground: Battleground): Option[Target] = {
			val backItem = "[-1: go back]"
			val possibleTargets = allTargets.filter { case (target, name) => skill.targetMode.canTarget(user, target, battleground) }
			val targetItems = possibleTargets.zipWithIndex map {
				case ((target, name), index) => s"[$index: $name]"
			}
			val itemsMsg = (backItem :: targetItems).mkString(" ")
			println("Pick a target: " + itemsMsg)

			try {
				val i = readInt
				if (i == -1) None
				else Some(possibleTargets(i)._1)
			} catch {
				case t: Throwable =>
					println("invalid response")
					promptTarget(skill, user, battleground)
			}
		}

		def promptSkillTarget(user: Combattant, battleground: Battleground): (Skill, Target) = {
			printStatus
			val skill = promptSkill
			promptTarget(skill, user, battleground) match {
				case None => promptSkillTarget(user, battleground)
				case Some(target) => skill -> target
			}
		}

		// TODO: make this an async event handler
		val skillSelectionHandler = new SyncEventHandler {
			def priority = Priority(10)
			def handlePreEvent(context: Battleground) = PartialFunction.empty
			def handlePostEvent(battleground: Battleground) = {
				case TurnBegin => {
					val (skill, target) = promptSkillTarget(hero, battleground)
					val s = CombattantAction(hero, SkillUse(skill, target))
					reactions { s }
				}
			}
		}

		object XAxis extends Axis[Int] {
			override def toString = "x"
		}

		val targetPositions = BidiMap[Target, Position](
			CombattantTarget(hero) -> PartyAxis(Party.BlueParty) ~ XAxis(1),
			CombattantTarget(ally) -> PartyAxis(Party.BlueParty) ~ XAxis(2),
			CombattantTarget(enemy) -> PartyAxis(Party.RedParty) ~ XAxis(1))

		println(targetPositions)

		val q = new EventProcessor(Set(skillSelectionHandler, SkillProcessor, new ResourceModificationProcessor), Battleground(targetPositions, BattleModifiers.empty))

		def checkEnd(battle: Battleground): Boolean = {
			enemy.getResource(HP).isEmpty ||
				(hero.getResource(HP).isEmpty && ally.getResource(HP).isEmpty)
		}

		def runBattle(q: EventProcessor): Future[EventProcessor] = {
			import scala.async.Async.{ async, await }

			def doTurnBegin(q0: EventProcessor): Future[EventProcessor] = async {
				println("Turn Begins!")
				val q1 = await { q0.process(TurnBegin) }
				if (checkEnd(q1.battleground)) q1
				else await { doTurnEnd(q1) }
			}

			def doTurnEnd(q0: EventProcessor): Future[EventProcessor] = async {
				println("Turn Ends...")
				val q1 = await { q0.process(TurnEnd) }
				if (checkEnd(q1.battleground)) q1
				else await { doTurnBegin(q1) }
			}

			doTurnBegin(q)
		}

		//		val events = for { i <- 1 to 5; e <- List(TurnBegin, TurnEnd) } yield e
		//		println(events)
		val end = runBattle(q) //q.processAll(events: _*)
		end.onComplete {
			case Failure(e) => e.printStackTrace()
			case Success(r) => println(r)
		}
		Await.ready(end, Duration.Inf)
		println(end.value)
		println("<done>")

	}
}