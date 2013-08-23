package io.dylemma.battle

trait EventProcessor extends Ordered[EventProcessor] {
	def process(qe: QueuedEvent): Unit
	def priority: Int
	def compare(that: EventProcessor) = {
		val pdif = this.priority - that.priority
		if (pdif != 0) pdif
		else this.## - that.##
	}
}

trait Expiring extends EventProcessor { Self =>
	def expiresAfter: Expiration
	// example: def expiresAfter = 2 eventsLike { case TurnEnd => true }

	private val expiration = expiresAfter

	abstract override def process(qe: QueuedEvent) = qe.event match {
		case EventProcessorRemoved(Self) =>
			expiration.expire
		case e =>
			expiration.process(e)
			if (expiration.shouldExpire) {
				trace(s"$this should expire now")
				qe.append(EventProcessorRemoved(this))
			}
			super.process(qe)
	}

	trait Expiration {
		private var _expired = false
		def isExpired: Boolean = _expired
		def expire: Unit = { _expired = true }

		def shouldExpire: Boolean
		def process(event: Event): Unit
	}

	class ExpireAfterSomeEvents(count: Int, predicate: PartialFunction[Event, Boolean]) extends Expiration {
		var current = 0
		val inc = predicate.lift.andThen {
			case Some(true) => 1
			case _ => 0
		}
		def process(event: Event) = {
			if (!isExpired) current += inc(event)
		}
		def shouldExpire = { current == count }
	}

	implicit class IntToExpirationBuilder(count: Int) {
		def eventsLike(predicate: PartialFunction[Event, Boolean]) =
			new ExpireAfterSomeEvents(count, predicate)
		def occurrancesOf(anEvent: Event) =
			new ExpireAfterSomeEvents(count, { case `anEvent` => true })
	}

}