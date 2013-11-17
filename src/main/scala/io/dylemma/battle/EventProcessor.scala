package io.dylemma.battle

import EventHandlerHelpers._
import scala.util.DynamicVariable
import scala.util.continuations._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.async.Async.{ async, await }
import io.dylemma.util.AsyncHelpers._

case class EventProcessor(handlers: Set[EventHandler], battleground: Battleground) {

	import battleground.Implicits._

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
					if (newProcessor.battleground.isFinished) {
						Nil -> newProcessor
					} else {
						val postReactions = await { newProcessor.getPostReactions(event) }
						val addedEvents = postReactions collect { case NewEvent(e) => e }
						addedEvents -> newProcessor
					}
			}
		}
	}

	protected def getPreReaction(event: Option[Event])(implicit exc: ExecutionContext): Future[Option[Event]] = {
		val handlersSorted = handlers.toList.sortBy(_.priority)

		handlersSorted.futureFoldLeft(event) {
			case (None, _) => Future.successful(None)
			case (Some(e), handler) => async {
				await { handler.getPreReactions(e) } match {
					case None => Some(e)
					case Some(CancelEvent) => None
					case Some(ReplaceEvent(r)) => Some(r)
				}
			}
		}

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
		case BattleEnd =>
			val b = battleground.copy(isFinished = true)
			this.copy(battleground = b)
		case _ => this
	}

	protected type FuturePosts = Future[List[PostEventReaction]]

	protected def getPostReactions(event: Event)(implicit exc: ExecutionContext): FuturePosts = {
		val handlersSorted = handlers.toList.sortBy { _.priority }

		handlersSorted.futureFoldLeft(Nil: List[PostEventReaction]) { (reactions, nextHandler) =>
			async {
				reactions ++ await { nextHandler.getPostReactions(event) }
			}
		}

	}

}