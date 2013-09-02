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

trait Expiring extends EventProcessor { Self =>
	def expiresAfter: Expiration
	// example: def expiresAfter = 2 eventsLike { case TurnEnd => true }

	private val expiration = expiresAfter

	//	abstract override def process(qe: QueuedEvent) = qe.event match {
	//		case EventProcessorRemoved(Self) =>
	//			expiration.expire
	//		case e =>
	//			expiration.process(e)
	//			if (expiration.shouldExpire) {
	//				trace(s"$this should expire now")
	//				qe.append(EventProcessorRemoved(this))
	//			}
	//			super.process(qe)
	//	}

	abstract override def process(implicit exc: ExecutionContext) = {
		case EventProcessorRemoved(Self) => expiration.expire
		case event =>
			expiration process event
			if (expiration.shouldExpire) {
				trace(s"$this should expire now")
				reactions + EventProcessorRemoved(this)
			} else {
				super.process.lift(event).getOrElse(reactions)
			}
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
		def shouldExpire = { current > count }
	}

	implicit class IntToExpirationBuilder(count: Int) {
		def eventsLike(predicate: PartialFunction[Event, Boolean]) =
			new ExpireAfterSomeEvents(count, predicate)
		def occurrancesOf(anEvent: Event) =
			new ExpireAfterSomeEvents(count, { case `anEvent` => true })
	}

}