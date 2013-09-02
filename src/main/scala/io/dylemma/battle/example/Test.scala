package io.dylemma.battle.example

import io.dylemma.battle._
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

object Test {

	case object Tick extends Event

	trait BaseTicker extends EventProcessor {
		def priority = 1
		override def process(implicit exc: ExecutionContext) = {
			case TurnBegan => Tick
		}
	}

	case class Ticker(duration: Int) extends BaseTicker with Expiring {
		def expiresAfter = duration occurrancesOf TurnBegan
	}

	case object BurnDamage extends Event

	trait BaseBurnProcessor extends EventProcessor {
		def priority = 0
		override def process(implicit exc: ExecutionContext) = {
			case TurnEnded => BurnDamage
		}
	}

	case class Burned(duration: Int) extends BaseBurnProcessor with Expiring {
		def expiresAfter = duration occurrancesOf TurnEnded
	}

	def main(args: Array[String]): Unit = {
		logThreshold = Trace
		val eq = new EventQueue(Ticker(2), Burned(1))

		val end = eq.runEventQueue(List(
			TurnBegan, TurnEnded,
			TurnBegan, TurnEnded,
			TurnBegan, TurnEnded,
			TurnBegan, TurnEnded))

		Await.ready(end, 5.seconds)
	}

}