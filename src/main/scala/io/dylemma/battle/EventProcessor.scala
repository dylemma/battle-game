package io.dylemma.battle

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import language.implicitConversions

sealed trait EventProcessorReaction {
	def ++(that: EventProcessorReaction)(implicit exc: ExecutionContext): EventProcessorReaction
	def asFuture: Future[EventReactions]
}

case class EventReactions(isCanceled: Boolean, appendedEvents: List[Event]) extends EventProcessorReaction {
	def cancel = copy(isCanceled = true)
	def +(event: Event) = copy(appendedEvents = this.appendedEvents :+ event)

	def ++(that: EventProcessorReaction)(implicit exc: ExecutionContext) = that match {
		case that: EventReactions => this :+ that
		case EventReactionsFuture(rFuture) => EventReactionsFuture(rFuture map { this :+ _ })
	}

	def :+(that: EventReactions): EventReactions = {
		EventReactions(
			this.isCanceled || that.isCanceled,
			this.appendedEvents ++ that.appendedEvents)
	}

	def asFuture = Future.successful(this)
}

case class EventReactionsFuture(reactions: Future[EventReactions]) extends EventProcessorReaction {

	def ++(that: EventProcessorReaction)(implicit exc: ExecutionContext) = that match {
		case that: EventReactions =>
			EventReactionsFuture(for (r <- reactions) yield r :+ that)
		case EventReactionsFuture(thatF) =>
			val newF = for {
				thisF <- reactions
				that <- thatF
			} yield thisF :+ that
			EventReactionsFuture(newF)
	}

	def asFuture = reactions

}

object EventProcessor {
	type Process = PartialFunction[Event, EventProcessorReaction]
}

trait EventProcessor extends Ordered[EventProcessor] {
	def process(implicit exc: ExecutionContext): EventProcessor.Process
	def priority: Int
	def compare(that: EventProcessor) = {
		val pdif = this.priority - that.priority
		if (pdif != 0) pdif
		else this.## - that.##
	}

	protected def reactions = EventReactions(false, Nil)

	implicit def eventReactionFuture(r: Future[EventReactions]): EventProcessorReaction =
		EventReactionsFuture(r)
	implicit def unitToReactions(u: Unit): EventProcessorReaction = reactions
	implicit def eventToReactions(e: Event): EventProcessorReaction = EventReactions(false, List(e))
	implicit def futureEventToReactions(e: Future[Event])(implicit exc: ExecutionContext): EventProcessorReaction =
		EventReactionsFuture(for (event <- e) yield EventReactions(false, List(event)))
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