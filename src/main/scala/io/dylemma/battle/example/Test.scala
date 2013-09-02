package io.dylemma.battle.example

import io.dylemma.battle._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

object Test {

	case object Tick extends Event
	case object BurnDamage extends Event

	/** Adds a `Tick` after each `TurnBegan`. Expires after `<duration>` Ticks.
	  */
	case class Ticker(duration: Int) extends ExpiringEventProcessor {
		def priority = 1
		private val exp = new ExpirationTicker(duration)
		def process(implicit exc: ExecutionContext) = {
			case TurnBegan => if (exp.check) expireMe else Tick
			case Tick => if (exp.tickAndCheck) expireMe
		}
	}

	/** Adds a `BurnDamage` after each `TurnEnded`. Expires after `<duration>`
	  * turn ends have passed.
	  */
	case class Burned(duration: Int) extends ExpiringEventProcessor {
		def priority = 0
		private val exp = new ExpirationTicker(duration)
		def process(implicit exc: ExecutionContext) = {
			case TurnEnded => if (exp.check) expireMe else { exp.tick; BurnDamage }
		}
	}

	def main(args: Array[String]): Unit = {
		logThreshold = Trace
		val eq = new EventQueue(Ticker(1), Burned(1))

		val end = eq.runEventQueue(List(
			TurnBegan, TurnEnded,
			TurnBegan, TurnEnded,
			TurnBegan, TurnEnded,
			TurnBegan, TurnEnded))

		Await.ready(end, 5.seconds)
	}

}