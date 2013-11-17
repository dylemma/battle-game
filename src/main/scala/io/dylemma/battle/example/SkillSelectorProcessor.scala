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
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.actor.ActorRef
import akka.util.Timeout

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

		def promptTarget(skill: Skill, user: Combattant, context: BattleContext): Option[Target] = {
			val backItem = "[-1: go back]"
			val possibleTargets = allTargets.filter { case (target, name) => skill.targetMode.canTarget(user, target, context) }
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
					promptTarget(skill, user, context)
			}
		}

		def promptSkillTarget(user: Combattant, context: BattleContext): (Skill, Target) = {
			printStatus
			val skill = promptSkill
			promptTarget(skill, user, context) match {
				case None => promptSkillTarget(user, context)
				case Some(target) => skill -> target
			}
		}

		// TODO: make this an async event handler
		val skillSelectionHandler = new SyncEventHandler {
			def priority = Priority(10)
			def handlePreEvent(context: BattleContext) = PartialFunction.empty
			def handlePostEvent(context: BattleContext) = {
				case TurnBegin => {
					val (skill, target) = promptSkillTarget(hero, context)
					val s = CombattantAction(hero, SkillUse(skill, target))
					reactions { s }
				}
			}
		}

		/** Checks to see if either party is dead; if so, it reacts with a
		  * BattleEnd event.
		  */
		val battleEndHandler = new SyncEventHandler {
			def priority = Priority()
			def handlePreEvent(context: BattleContext) = PartialFunction.empty
			def handlePostEvent(context: BattleContext) = {
				case _ => {
					val isOver = (enemy.getResource(HP).isEmpty) ||
						(hero.getResource(HP).isEmpty && ally.getResource(HP).isEmpty)
					if (isOver) reactions { BattleEnd } else noReactions
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

		val actorSystem = ActorSystem("BattleExample")
		val battleActor = {
			val a = actorSystem.actorOf(Props[Battle])

			val handlers = List(
				skillSelectionHandler, battleEndHandler, SkillProcessor, new ResourceModificationProcessor)
			for (handler <- handlers) a ! Battle.Update(AddEventHandler(handler))

			val combattants = Map(
				hero -> PartyAxis(Party.BlueParty) ~ XAxis(1),
				ally -> PartyAxis(Party.BlueParty) ~ XAxis(2),
				enemy -> PartyAxis(Party.RedParty) ~ XAxis(1))

			for { (cmb, pos) <- combattants }
				a ! Battle.Update(TargetMoved(CombattantTarget(cmb), Position.empty, pos))

			a
		}

		def runBattle(battleActor: ActorRef): Future[BattleContext] = {
			import scala.async.Async.{ async, await }
			implicit val timeout = Timeout(10.seconds)

			async {
				val ctx = await { (battleActor ? Battle.GetContext).mapTo[BattleContext] }

				println("running battle")
				await { battleActor ? BattleBegin }
				println("battle begain")

				def doTurnBegin(): Future[BattleContext] = async {
					println("turn begins!")
					if (ctx.isFinished) ctx
					else {
						val ack = await { battleActor ? TurnBegin }
						await { doTurnEnd() }
					}
				}

				def doTurnEnd(): Future[BattleContext] = async {
					println("turn ended...")
					if (ctx.isFinished) ctx
					else {
						val ack = await { battleActor ? TurnBegin }
						await { doTurnBegin() }
					}
				}

				await { doTurnBegin() }
				ctx
			}
		}

		val end = runBattle(battleActor)
		end.onComplete { value =>
			value match {
				case Failure(e) => println(s"Battle failed to finish: $e")
				case Success(r) => println(s"Battle finished with result: $r")
			}
			actorSystem.shutdown()
			println("<done>")

			import collection.JavaConverters._
			for { (thread, stack) <- Thread.getAllStackTraces.asScala if !thread.isDaemon && thread.isAlive } {
				println(s"Still alive non-daemon thread: $thread")
			}
		}

	}
}