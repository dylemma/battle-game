package io.dylemma.battle

import scala.annotation.tailrec
import scala.collection.mutable.Queue
import scala.collection.mutable.SortedSet
import akka.actor.Actor
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success
import scala.concurrent.Await

class EventQueue(processors: SortedSet[EventProcessor]) {
	def this(pp: EventProcessor*) = this(SortedSet(pp: _*))
	def this(pp: List[EventProcessor]) = this(SortedSet(pp: _*))

	def runEventQueue(q: List[Event], callback: Event => Future[Unit] = { event => Future.successful() })(implicit exc: ExecutionContext): Future[EventQueue] = {
		runEventQueue(q, callback, processors)
	}

	private def runEventQueue(q: List[Event], callback: Event => Future[Unit], processors: SortedSet[EventProcessor])(implicit exc: ExecutionContext): Future[EventQueue] = q match {

		// Queue is empty
		case Nil => Future successful this

		// Special Case:
		// ignore events that would remove a processor that doesn't exist
		case EventProcessorRemoved(p) :: tail if !(processors contains p) => runEventQueue(tail, callback, processors)

		// Normal Operation:
		// send event through processors for "cancel" and "append" ops
		case event :: laterEvents => {
			def singleProcess(proc: EventProcessor) = {
				proc.process.lift(event).getOrElse { EventReactions(false, Nil) }
			}

			def process(procs: List[EventProcessor]): EventProcessorReaction = procs match {
				case Nil => EventReactions(false, Nil)
				case p :: procsTail => singleProcess(p) match {
					case r: EventReactions if r.isCanceled => r
					case r: EventReactions => r ++ process(procsTail)
					case EventReactionsFuture(rFuture) => EventReactionsFuture(rFuture flatMap {
						case r if r.isCanceled => Future.successful(r)
						case r => { r ++ process(procsTail) } match {
							case r: EventReactions => Future.successful(r)
							case EventReactionsFuture(r) => r
						}
					})
				}
			}

			def doReactions(event: Event, reactions: EventReactions): Future[SortedSet[EventProcessor]] = {
				if (reactions.isCanceled) Future successful processors
				else {
					debug(s"Event Happened: $event")
					for {
						_ <- callback(event)
					} yield event match {
						case EventProcessorAdded(p) => processors + p
						case EventProcessorRemoved(p) => processors - p
						case _ => processors
					}
				}
			}

			//			process(processors.toList).asFuture flatMap { reactions =>
			//				if (!reactions.isCanceled) eventHappened(event)
			//
			//				runEventQueue(reactions.appendedEvents ++ laterEvents)
			//			}

			for {
				reactions <- process(processors.toList).asFuture
				newProcessors <- doReactions(event, reactions)
				newQueue <- runEventQueue(reactions.appendedEvents ++ laterEvents, callback, newProcessors)
			} yield newQueue
		}
	}

	def eventHappened(event: Event) = {
		debug("Event Happened:", event)
		event match {
			case EventProcessorAdded(p) => processors += p
			case EventProcessorRemoved(p) => processors -= p
			case _ => // no op
		}
	}
}

object EventQueue {
	case object EventA extends Event
	case object EventB extends Event
	case object EventC extends Event

	case object EventAReaction extends Event
	val eventAReactor = new EventProcessor {
		override def toString = "eventAReactor"
		val priority = 0
		def process(implicit exc: ExecutionContext) = {
			case EventA => EventAReaction
		}
	}

	val eventBProcessor = new EventProcessor {
		override def toString = "eventBProcessor"
		val priority = 0
		def process(implicit exc: ExecutionContext) = {
			case EventB => EventProcessorAdded(eventAReactor)
		}
	}

	case object EventCReplacement extends Event

	val eventCProcessor = new EventProcessor {
		override def toString = "eventCProcessor"
		val priority = 0
		def process(implicit exc: ExecutionContext) = {
			case EventC => EventCReplacement
		}
	}

	def main(args: Array[String]): Unit = {
		import ExecutionContext.Implicits.global
		import concurrent.duration._

		val eq = new EventQueue(eventBProcessor, eventCProcessor)
		val callback = (event: Event) => Future {
			print("wait for it... ")
			Thread.sleep(500)
			println(s"Event happened: $event")
		}
		val end = eq.runEventQueue(List(TurnBegan, EventA, EventB, EventA, EventC, TurnEnded), callback)
		Await.ready(end, 5.seconds)
	}
}