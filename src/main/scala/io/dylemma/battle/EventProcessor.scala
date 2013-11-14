package io.dylemma.battle

import EventHandlerHelpers._
import scala.util.DynamicVariable
import scala.util.continuations._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.async.Async.{ async, await }

case class EventProcessor(handlers: Set[EventHandler], battleground: Battleground) {

	def process(event: Event)(implicit exc: ExecutionContext): Future[EventProcessor] = {
		def innerProcess(events: List[Event], processor: EventProcessor): Future[EventProcessor] = events match {
			case Nil => Future.successful(processor)
			case event :: moreEvents => for {
				(addedEvents, nextProcessor) <- processor.processSingle(event)
				result <- {
					val nextEvents = (moreEvents ++ addedEvents).sortBy { _.calculatePriority(nextProcessor.battleground) }
					innerProcess(nextEvents, nextProcessor)
				}
			} yield result
		}

		innerProcess(event :: Nil, this)
	}

	def processAll(events: Event*)(implicit exc: ExecutionContext): Future[EventProcessor] = {
		def fold(processor: EventProcessor, events: List[Event]): Future[EventProcessor] = events match {
			case Nil => Future.successful(processor)
			case event :: nextEvents => for {
				nextProcessor <- processor.process(event)
				result <- fold(nextProcessor, nextEvents)
			} yield result
		}

		fold(this, events.toList)
	}

	protected def processSingle(event: Event)(implicit exc: ExecutionContext): Future[(List[Event], EventProcessor)] = {
		val handlersSorted = handlers.toList.sortBy(_.priority)
		async {
			val readyEvent = await { getPreReaction(Some(event)) }
			readyEvent match {
				case None => Nil -> this
				case Some(event) =>
					val newProcessor = updateForEvent(event)
					val postReactions = await { newProcessor.getPostReactions(event) }
					val addedEvents = postReactions collect { case NewEvent(e) => e }
					addedEvents -> newProcessor
			}
		}
	}

	protected def handlePre(handler: EventHandler, event: Event)(implicit exc: ExecutionContext): Future[Option[PreEventReaction]] = {
		val pf = handler match {
			case sync: SyncEventHandler => sync.handlePreEvent(battleground)
			case async: AsyncEventHandler => async.handlePreEvent(battleground)
		}
		if (pf.isDefinedAt(event)) pf(event).asFuture
		else Future.successful(None)
	}

	protected def getPreReaction(event: Option[Event])(implicit exc: ExecutionContext): Future[Option[Event]] = {
		val handlersSorted = handlers.toList.sortBy(_.priority)

		def recurse(event: Option[Event], handlers: List[EventHandler]): Future[Option[Event]] = async {
			handlers match {
				case Nil => event
				case handler :: nextHandlers => event match {
					case None => None
					case Some(e) => await { handlePre(handler, e) } match {
						case None => await { recurse(event, nextHandlers) }
						case Some(CancelEvent) => None
						case Some(ReplaceEvent(rep)) => await { recurse(Some(rep), nextHandlers) }
					}
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

	protected def handlePost(handler: EventHandler, event: Event)(implicit exc: ExecutionContext) = {
		val pf = handler match {
			case sync: SyncEventHandler => sync.handlePostEvent(battleground)
			case async: AsyncEventHandler => async.handlePostEvent(battleground)
		}
		if (pf.isDefinedAt(event)) pf(event).asFuture
		else Future.successful(Nil)
	}

	protected type FuturePosts = Future[List[PostEventReaction]]
	protected def getPostReactions(event: Event)(implicit exc: ExecutionContext): FuturePosts = {
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