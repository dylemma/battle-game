package io.dylemma.battle

import scala.concurrent.Future

sealed trait PreEventReaction
case object CancelEvent extends PreEventReaction
case class ReplaceEvent(replacement: Event) extends PreEventReaction

sealed trait PostEventReaction
case class NewEvent(event: Event) extends PostEventReaction

trait EventHandler {
	def priority: Priority
	def handlePreEvent(mods: BattleModifiers): PartialFunction[Event, Option[PreEventReaction]]
	def handlePostEvent(mods: BattleModifiers): PartialFunction[Event, List[PostEventReaction]]
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