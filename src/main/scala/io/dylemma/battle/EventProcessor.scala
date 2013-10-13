package io.dylemma.battle

import EventHandlerHelpers._
import scala.util.DynamicVariable

//TODO: test that this works...

object EventProcessor {
	private[EventProcessor] val callbackVar: DynamicVariable[Event => Unit] =
		new DynamicVariable(_ => ())
}

case class EventProcessor(handlers: Set[EventHandler], battleground: Battleground) {

	private def eventCallback = EventProcessor.callbackVar.value

	def withEventCallback[T](callback: Event => Unit)(body: => T): T =
		EventProcessor.callbackVar.withValue(callback)(body)

	def process(event: Event): EventProcessor = {
		def innerProcess(events: List[Event], processor: EventProcessor): EventProcessor = events match {
			case Nil => processor
			case event :: moreEvents =>
				val (addedEvents, newProcessor) = processor.processSingle(event)
				val nextEvents = (moreEvents ++ addedEvents).sortBy { _.calculatePriority(newProcessor.battleground) }
				innerProcess(nextEvents, newProcessor)
		}
		innerProcess(event :: Nil, this)
	}

	def processAll(events: Event*): EventProcessor = {
		events.foldLeft(this) { _ process _ }
	}

	protected def processSingle(event: Event): (List[Event], EventProcessor) = {
		val handlersSorted = handlers.toList.sortBy(_.priority)
		val readyEvent = getPreReaction(Some(event))
		readyEvent match {
			case None => Nil -> this
			case Some(event) =>
				eventCallback(event)
				val newProcessor = updateForEvent(event)
				val postReactions = newProcessor.getPostReactions(event)
				val addedEvents = postReactions collect { case NewEvent(e) => e }
				addedEvents -> newProcessor
		}
	}

	protected def getPreReaction(event: Option[Event]): Option[Event] = {
		val handlersSorted = handlers.toList.sortBy(_.priority)

		def recurse(event: Option[Event], handlers: List[EventHandler]): Option[Event] = handlers match {
			case Nil => event
			case handler :: nextHandlers => event flatMap { e =>
				val r = handler.handlePreEvent(battleground).lift(e).flatten
				r match {
					case None => recurse(event, nextHandlers)
					case Some(CancelEvent) => None
					case Some(ReplaceEvent(rep)) => recurse(Some(rep), nextHandlers)
				}
			}
		}

		recurse(event, handlersSorted)
	}

	protected def updateForEvent(event: Event) = event match {
		case AddBattleModifier(mod) =>
			val m = battleground.modifiers + mod
			val b = battleground.copy(modifiers = m)
			this.copy(battleground = b)

		case RemoveBattleModifier(mod) =>
			val m = battleground.modifiers - mod
			val b = battleground.copy(modifiers = m)
			this.copy(battleground = b)

		case AddEventHandler(handler) => this.copy(handlers = handlers + handler)
		case RemoveEventHandler(handler) => this.copy(handlers = handlers - handler)
		case _ => this
	}

	protected def getPostReactions(event: Event): List[PostEventReaction] = {
		val noReactions: List[PostEventReaction] = Nil
		handlers.foldLeft(noReactions) { (reactions, handler) =>
			val r2 = handler.handlePostEvent(battleground).lift(event) getOrElse Nil
			reactions ++ r2
		}
	}
}