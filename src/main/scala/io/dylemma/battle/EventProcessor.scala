package io.dylemma.battle

import EventHandlerHelpers._
import scala.util.DynamicVariable
import scala.util.continuations._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

case class EventProcessor(handlers: Set[EventHandler], battleground: Battleground) {

	def processFuture(event: Event)(implicit exc: ExecutionContext): Future[EventProcessor] = {
		def innerProcess(events: List[Event], processor: EventProcessor): Future[EventProcessor] = events match {
			case Nil => Future.successful(processor)
			case event :: moreEvents => for {
				(addedEvents, nextProcessor) <- processor.processSingleFuture(event)
				result <- {
					val nextEvents = (moreEvents ++ addedEvents).sortBy { _.calculatePriority(nextProcessor.battleground) }
					innerProcess(nextEvents, nextProcessor)
				}
			} yield result
		}

		innerProcess(event :: Nil, this)
	}

	def processAllFuture(events: Event*)(implicit exc: ExecutionContext): Future[EventProcessor] = {
		def fold(processor: EventProcessor, events: List[Event]): Future[EventProcessor] = events match {
			case Nil => Future.successful(processor)
			case event :: nextEvents => for {
				nextProcessor <- processor.processFuture(event)
				result <- fold(nextProcessor, nextEvents)
			} yield result
		}

		fold(this, events.toList)
	}

	protected def processSingleFuture(event: Event)(implicit exc: ExecutionContext): Future[(List[Event], EventProcessor)] = {
		val handlersSorted = handlers.toList.sortBy(_.priority)
		val readyEvent = getPreReactionFuture(Some(event))
		readyEvent flatMap {
			case None => Future.successful(Nil -> this)
			case Some(event) => for {
				newProcessor <- Future successful updateForEvent(event)
				postReactions <- newProcessor.getPostReactionsFuture(event)
			} yield {
				val addedEvents = postReactions collect { case NewEvent(e) => e }
				addedEvents -> newProcessor
			}
		}
	}

	protected def handlePre(handler: EventHandler, event: Event): Future[Option[PreEventReaction]] = {
		val pf = handler.handlePreEvent(battleground)
		if (pf.isDefinedAt(event)) pf(event).asFuture
		else Future.successful(None)
	}

	protected def getPreReactionFuture(event: Option[Event])(implicit exc: ExecutionContext): Future[Option[Event]] = {
		val handlersSorted = handlers.toList.sortBy(_.priority)

		def recurse(event: Option[Event], handlers: List[EventHandler]): Future[Option[Event]] = handlers match {
			case Nil => Future.successful(event)
			case handler :: nextHandlers => event match {
				case None => Future.successful(None)
				case Some(e) => handlePre(handler, e) flatMap {
					case None => recurse(event, nextHandlers)
					case Some(CancelEvent) => Future.successful(None)
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

	protected def handlePost(handler: EventHandler, event: Event) = {
		val pf = handler.handlePostEvent(battleground)
		if (pf.isDefinedAt(event)) pf(event).asFuture
		else Future.successful(Nil)
	}

	protected type FuturePosts = Future[List[PostEventReaction]]
	protected def getPostReactionsFuture(event: Event)(implicit exc: ExecutionContext): FuturePosts = {
		def fold(reactions: FuturePosts, handlers: List[EventHandler]): FuturePosts = handlers match {
			case Nil => reactions
			case handler :: nextHandlers =>
				val nextReactions = for {
					accum <- reactions
					append <- handlePost(handler, event)
				} yield accum ++ append
				fold(nextReactions, nextHandlers)
		}

		fold(Future.successful(Nil), handlers.toList.sortBy { _.priority })
	}
}