package io.dylemma.battle

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import language.implicitConversions

object EventProcessor {
	type Process = PartialFunction[Event, EventProcessorReaction]
}

trait EventProcessor extends Ordered[EventProcessor] {
	def process(implicit exc: ExecutionContext): EventProcessor.Process
	def priority: Int
	def compare(that: EventProcessor) = {
		// order descending by priority: highest priority comes first
		val pdif = that.priority - this.priority
		if (pdif != 0) pdif
		else this.## - that.##
	}

	protected def reactions = EventReactions()

	implicit def eventReactionFuture(r: Future[EventReactions]): EventProcessorReaction =
		EventReactionsFuture(r)
	implicit def unitToReactions(u: Unit): EventProcessorReaction = reactions
	implicit def eventToReactions(e: Event): EventProcessorReaction = EventReactions(appendedEvents = List(e))
	implicit def futureEventToReactions(e: Future[Event])(implicit exc: ExecutionContext): EventProcessorReaction =
		EventReactionsFuture(for (event <- e) yield EventReactions(appendedEvents = List(event)))
}

trait ExpiringEventProcessor extends EventProcessor {

	class ExpirationTicker(ticks: Int) {
		private var t = 0
		def tickAndCheck: Boolean = { tick; check }
		def check: Boolean = { t >= ticks }
		def tick: Unit = { t += 1 }
	}

	def expireMe: Event = EventProcessorRemoved(this)

}