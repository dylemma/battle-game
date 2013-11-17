package io.dylemma.battle

import io.dylemma.util.BidiMap
import akka.actor.Actor
import akka.actor.Stash
import scala.concurrent.Future
import scala.util.Success
import akka.actor.Status
import scala.util.Failure
import scala.concurrent.ExecutionContext

import scala.async.Async.{ async, await }
import io.dylemma.util.AsyncHelpers._

case class Battleground(targetPositions: BidiMap[Target, Position], modifiers: BattleModifiers, isFinished: Boolean = false) extends BattleContext {

	def positionOf(target: Target): Option[Position] = targetPositions.fromLeft(target)
	def targetAt(pos: Position): Option[Target] = targetPositions.fromRight(pos)

	def combattantAt(pos: Position): Option[Combattant] = targetAt(pos).collect {
		case CombattantTarget(cmb) => cmb
	}
}

/** A limited view of a `Battle` instance.
  * Contents TBD.
  */
trait BattleContext { self: BattleContext =>
	def positionOf(target: Target): Option[Position]
	def positionOf(cmb: Combattant): Option[Position] = positionOf { CombattantTarget(cmb) }

	def isFinished: Boolean

	object Implicits {
		implicit class EventHandlerInContext(handler: EventHandler) {
			def getPreReactions(event: Event)(implicit exc: ExecutionContext): Future[Option[PreEventReaction]] = {
				val pf = handler match {
					case sync: SyncEventHandler => sync.handlePreEvent(self)
					case async: AsyncEventHandler => async.handlePreEvent(self)
				}
				if (pf isDefinedAt event) pf(event).asFuture
				else Future.successful(None)
			}

			def getPostReactions(event: Event)(implicit exc: ExecutionContext): Future[List[PostEventReaction]] = {
				val pf = handler match {
					case sync: SyncEventHandler => sync.handlePostEvent(self)
					case async: AsyncEventHandler => async.handlePostEvent(self)
				}
				if (pf isDefinedAt event) pf(event).asFuture
				else Future.successful(Nil)
			}
		}
	}
}

object Battle {
	case object Ack

	/** Send to a Battle actor to cause it to update its internal
	  * state based on the `event`, bypassing any current handlers
	  * or state logic.
	  */
	case class Update(event: Event)

	/** Send to a Battle actor to get a `BattleContext` instance in reply.
	  */
	case object GetContext
}

class Battle extends Actor with Stash {
	private val targetMap = collection.mutable.Map[Target, Position]()
	private val modifiers = collection.mutable.Set[BattleModifier]()
	private val eventHandlers = collection.mutable.Set[EventHandler]()

	import context.dispatcher

	object battleContext extends BattleContext {
		def positionOf(target: Target) = targetMap get target

		private var _finished = false
		def isFinished = _finished
		private[Battle] def isFinished_=(f: Boolean) { _finished = f }
	}

	def receive = {
		case Battle.Update(event) =>
			println(s"Got initialization event: $event")
			updateFromEvent(event)
			sender ! Battle.Ack

		case Battle.GetContext =>
			println("Got request for BattleContext")
			sender ! battleContext

		case event: Event =>
			println(s"Got battle event: $event")
			val mySender = sender
			if (battleContext.isFinished) {
				mySender ! Status.Failure {
					new IllegalStateException("The battle is finished")
				}
			} else {
				context become StashingEvents

				def unblock() = {
					unstashAll()
					context.unbecome()
				}

				try {
					val f = runEventProcessing(event)
					f onComplete {
						case Success(u) =>
							unblock()
							mySender ! Status.Success(Battle.Ack)
						case Failure(t) =>
							unblock()
							mySender ! Status.Failure(t)
					}
				} catch {
					case t: Throwable =>
						unblock()
						mySender ! Status.Failure(t)
				}
			}
	}

	private val StashingEvents: Actor.Receive = {
		case x =>
			println("Received $x, but currently processing an event. Stashing it for later")
			stash()
	}

	private def runEventProcessing(event: Event): Future[Unit] = {
		def recurse(remainingEvents: List[Event]): Future[Unit] = async {
			remainingEvents match {
				case Nil => () // done
				case event :: moreEvents =>
					val addedEvents = await { processSingle(event) }
					if (!battleContext.isFinished) {

						/*
						 * TODO: this is where a check for the battle being done should go.
						 */
						val nextEvents = (moreEvents ++ addedEvents).sortBy { _.calculatePriority(battleContext) }
						await { recurse(nextEvents) }
					}
			}
		}

		recurse(event :: Nil)
	}

	/** Process the given `event`, allowing it to be modified by any
	  * preReactions before it actually happens. If/when it does happen,
	  * calculate and return any postReactions to that event. Any added
	  * events from postReactions will not be run by this method.
	  */
	private def processSingle(event: Event): Future[List[Event]] = async {
		await { getPreReactions(event) } match {
			case None => Nil
			case Some(event) =>
				// Now the `event` actually happens!
				updateFromEvent(event)

				val postReactions = await { getPostReactions(event) }
				val addedEvents = postReactions collect { case NewEvent(e) => e }

				// return the events that were added, so that they can be 
				// processed with another call to processSingle
				addedEvents
		}
	}

	/** Update the battle's internal state based on the `event`.
	  */
	private def updateFromEvent(event: Event): Unit = event match {
		case AddBattleModifier(mod) => modifiers += mod
		case RemoveBattleModifier(mod) => modifiers -= mod
		case AddEventHandler(h) => eventHandlers += h
		case RemoveEventHandler(h) => eventHandlers -= h
		case TargetMoved(target, from, to) => targetMap.put(target, to)
		case BattleEnd => battleContext.isFinished = true
		case _ => ()
	}

	/** Return a snapshot of the current `eventHandlers`,
	  * ordered by their priority.
	  */
	private def handlersSorted = {
		eventHandlers.toList.sortBy { _.priority }
	}

	/** Feed the `event` through the current `eventHandlers` for
	  * preReactions, which can potentially cancel or replace the
	  * event. Return a Future containing the altered event based
	  * on the reactions of the handlers.
	  */
	private def getPreReactions(event: Event): Future[Option[Event]] = {
		import battleContext.Implicits._

		handlersSorted.futureFoldLeft(Option { event }) {
			case (None, _) => Future.successful(None)
			case (Some(e), handler) => async {
				await { handler getPreReactions e } match {
					case None => Some(e)
					case Some(CancelEvent) => None
					case Some(ReplaceEvent(r)) => Some(r)
				}
			}
		}
	}

	/** Feed the `event` through the current `eventHandlers`,
	  * accumulating a list of their post-reactions. Return
	  * a Future that will complete once the list has been
	  * fully calculated.
	  */
	private def getPostReactions(event: Event): Future[List[PostEventReaction]] = {
		import battleContext.Implicits._

		handlersSorted.futureFoldLeft(List[PostEventReaction]()) {
			case (reactions, handler) => async {
				reactions ++ await { handler getPostReactions event }
			}
		}
	}
}