package io.dylemma.battle

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

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