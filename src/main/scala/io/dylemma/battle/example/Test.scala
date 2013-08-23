package io.dylemma.battle.example

import io.dylemma.battle._

object Test {

	case object Tick extends Event

	trait BaseTicker extends EventProcessor {
		def priority = 1
		override def process(qe: QueuedEvent) = qe.event match {
			case TurnBegan => qe.append(Tick)
			case _ =>
		}
	}

	case class Ticker(duration: Int) extends BaseTicker with Expiring {
		def expiresAfter = duration occurrancesOf TurnEnded
	}

	case object BurnDamage extends Event

	trait BaseBurnProcessor extends EventProcessor {
		def priority = 0
		override def process(qe: QueuedEvent) = {
			qe.event match {
				case TurnEnded =>
					qe.append(BurnDamage)
				case _ =>
			}
		}
	}

	case class Burned(duration: Int) extends BaseBurnProcessor with Expiring {
		def expiresAfter = duration occurrancesOf TurnEnded
	}

	def main(args: Array[String]): Unit = {
		logThreshold = Debug
		val eq = new EventQueue(Ticker(2), Burned(3))
		eq.enter(TurnBegan)
		eq.enter(TurnEnded)
		eq.enter(TurnBegan)
		eq.enter(TurnEnded)
		eq.enter(TurnBegan)
		eq.enter(TurnEnded)
		eq.enter(TurnBegan)
		eq.enter(TurnEnded)
	}

}