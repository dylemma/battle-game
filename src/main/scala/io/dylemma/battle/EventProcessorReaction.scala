package io.dylemma.battle

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

sealed trait EventProcessorReaction {
	def ++(that: EventProcessorReaction)(implicit exc: ExecutionContext): EventProcessorReaction
	def asFuture: Future[EventReactions]
}

case class EventReactions(
	isCanceled: Boolean = false,
	replacement: Option[Event] = None,
	appendedEvents: List[Event] = Nil)
	extends EventProcessorReaction {

	def cancel = copy(isCanceled = true)
	def +(event: Event) = copy(appendedEvents = this.appendedEvents :+ event)
	def replaceWith(event: Event) = copy(replacement = Some(event))

	def ++(that: EventProcessorReaction)(implicit exc: ExecutionContext) = that match {
		case that: EventReactions => this :+ that
		case EventReactionsFuture(rFuture) => EventReactionsFuture(rFuture map { this :+ _ })
	}

	def :+(that: EventReactions): EventReactions = {
		EventReactions(
			this.isCanceled || that.isCanceled,
			that.replacement orElse this.replacement, // `that`'s replacement should override this one
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