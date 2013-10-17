package io.dylemma.battle

import scala.concurrent.Future
import scala.util.continuations._

sealed trait PreEventReaction
case object CancelEvent extends PreEventReaction
case class ReplaceEvent(replacement: Event) extends PreEventReaction

sealed trait PostEventReaction
case class NewEvent(event: Event) extends PostEventReaction

case class PreEventReactions(asFuture: Future[Option[PreEventReaction]])
case class PostEventReactions(asFuture: Future[List[PostEventReaction]])

trait EventHandler {
	/** The priority of this handler. An EventProcessor will sort handlers and
	  * pass events through them in their sorted order.
	  */
	def priority: Priority

	/** React to an event before it happens, possibly by cancelling or replacing it.
	  *
	  * @param event The event that is about to happen
	  * @param context Contextual information about where the event is happening
	  * @return The reactions emitted by this handler in response to the event
	  */
	def handlePreEvent(context: Battleground): PartialFunction[Event, PreEventReactions]

	/** React to an event just after it happens, possibly adding new events.
	  *
	  * @param event The event that happened
	  * @param context Contextual information about where the event happened
	  * @return The reactions emitted by this handler in response to the event
	  */
	def handlePostEvent(context: Battleground): PartialFunction[Event, PostEventReactions]

}

object EventHandlerSyntax extends EventHandlerSyntax

/** Contains some helpful methods for generating Pre and Post EventReactions.
  * Provides `reactions` which accepts a variety of inputs and converts them to
  * either Pre or Post reactions.
  * Also provides `noReactions` which can return as either a Pre or Post reaction that
  * includes no actual reactions.
  */
trait EventHandlerSyntax {
	trait ReactionsShape[-T, +R] { def convert(thing: T): R }
	trait NoReactionsShape[+R] { def noReaction: R }

	def reactions[T, R](thing: T)(implicit shape: ReactionsShape[T, R]): R = shape.convert(thing)
	def noReactions[R](implicit shape: NoReactionsShape[R]) = shape.noReaction

	implicit object NoPreReactionsShape extends NoReactionsShape[PreEventReactions] {
		def noReaction = PreEventReactions(Future.successful(None))
	}
	implicit object NoPostReactionsShape extends NoReactionsShape[PostEventReactions] {
		def noReaction = PostEventReactions(Future.successful(Nil))
	}
	implicit object RawPreReactionsShape extends ReactionsShape[PreEventReaction, PreEventReactions] {
		def convert(raw: PreEventReaction) = PreEventReactions(Future.successful(Some(raw)))
	}
	implicit object EventPostReactionsShape extends ReactionsShape[Event, PostEventReactions] {
		def convert(event: Event) = PostEventReactions(Future.successful(List(NewEvent(event))))
	}
	implicit object EventListPostReactionsShape extends ReactionsShape[List[Event], PostEventReactions] {
		def convert(events: List[Event]) = PostEventReactions(Future.successful(events.map(NewEvent)))
	}
}

object EventHandlerHelpers extends EventHandlerHelpers
trait EventHandlerHelpers {
	implicit class PreEventReactionAnd(r: PreEventReaction) {
		def &(s: PreEventReaction) = List(r, s)
		def &(ss: List[PreEventReaction]) = r :: ss
	}
	implicit class PreEventReactionsAnd(rr: List[PreEventReaction]) {
		def &(s: PreEventReaction) = rr :+ s
		def &(ss: List[PreEventReaction]) = rr ++ ss
	}
	implicit class PostEventReactionAnd(r: PostEventReaction) {
		def &(s: PostEventReaction) = List(r, s)
		def &(ss: List[PostEventReaction]) = r :: ss
	}
	implicit class PostEventReactionsAnd(rr: List[PostEventReaction]) {
		def &(s: PostEventReaction) = rr :+ s
		def &(ss: List[PostEventReaction]) = rr ++ ss
	}
}

trait ExpirationHelpers {
	// return a reaction list that will remove the handler
	def expire(handler: EventHandler): List[PostEventReaction] =
		List(NewEvent(RemoveEventHandler(handler)))

	// A simple counter that counts up to n and can check if it's reached there yet
	class ExpirationCounter(n: Int) {
		private var current = 0
		def increment: Unit = { current += 1 }
		def check: Boolean = { current >= n }
		def incrementAndCheck: Boolean = { increment; check }
	}

	// DSL:
	// this expiresAfter 10 instancesOf TurnEnd
	// this expiresAfter 2 eventsLike { case Thing(_) => true }

	implicit class EventHandlerExpiresAfter(handler: EventHandler) {
		def expiresAfter(n: Int) = new ExpiresAfterN(handler, n)
	}

	class ExpiresAfterN(handler: EventHandler, n: Int) {
		def instancesOf(event: Event): Event => Boolean = {
			var counter = new ExpirationCounter(n)
			(e) => {
				if (e == event) counter.increment
				counter.check
			}
		}

		def eventsLike(filter: PartialFunction[Event, Boolean]): Event => Boolean = {
			var counter = new ExpirationCounter(n)
			val pred = (event: Event) => filter.lift(event) == Some(true)
			(e) => {
				if (pred(e)) counter.increment
				counter.check
			}
		}
	}
}