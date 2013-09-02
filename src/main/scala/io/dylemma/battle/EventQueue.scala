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

	/** Enter the given list of Events (q) into the queue. Each event will be processed using
	  * this EventQueue's `processors` set, possibly being canceled or modified along the way,
	  * or even having more events pushed in.
	  * @return A Future containing the next state of the EventQueue after the events have been
	  * processed. (Certain events may add or remove EventProcessors, which is essentially a
	  * state change. EventQueue is immutable, so the state change is represented by returning
	  * a new object).
	  */
	def runEventQueue(q: List[Event], callback: Event => Future[Unit] = { event => Future.successful() })(implicit exc: ExecutionContext): Future[EventQueue] = {
		runEventQueue(q, callback, processors)
	}

	// recursive helper method for the public `runEventQueue`
	private def runEventQueue(q: List[Event], callback: Event => Future[Unit], processors: SortedSet[EventProcessor])(implicit exc: ExecutionContext): Future[EventQueue] = q match {

		// Queue is empty
		case Nil => Future successful this

		// Special Case:
		// ignore events that would remove a processor that doesn't exist
		case EventProcessorRemoved(p) :: tail if !(processors contains p) => runEventQueue(tail, callback, processors)

		// Normal Operation:
		// send event through processors to get the accumulated reactions
		case event :: laterEvents => {

			for {
				reactions <- process(event, processors.toList).asFuture
				newProcessors <- doReactions(event, reactions, callback)
				newQueue <- runEventQueue(reactions.appendedEvents ++ laterEvents, callback, newProcessors)
			} yield newQueue
		}
	}

	/** Process the `event` based on the given `reactions` to it. Certain events may modify the
	  * set of EventProcessors, so this method returns the new set of processors. If the reactions
	  * canceled the event, this method simply returns immediately. If the reactions replaced the
	  * event, the replacement is used in place of the given `event`. The `eventCallback` will be
	  * run before returning the new set of processors; the processors are returned as a future
	  * that only returns after the `eventCallback` returns.
	  */
	private def doReactions(event: Event, reactions: EventReactions, eventCallback: Event => Future[Unit])(implicit exc: ExecutionContext): Future[SortedSet[EventProcessor]] = {
		if (reactions.isCanceled) Future successful processors
		else {
			// if there was a replacement, use that instead of the given `event`
			val actualEvent = reactions.replacement getOrElse event
			debug(s"Event Happened: $actualEvent")

			// run the event callback, then yield the new set of event processors in case
			// there was an addition or removal
			for {
				_ <- eventCallback(actualEvent)
			} yield event match {
				case EventProcessorAdded(p) => processors + p
				case EventProcessorRemoved(p) => processors - p
				case _ => processors
			}
		}
	}

	/** Send the given `event` through a list of EventProcessors, accumulating their reactions in order.
	  * The `procs` list is assumed to be pre-sorted by EventProcessor's natural ordering (by priority).
	  * If a processor chooses to cancel the event, no further processors will run.
	  * If a processor chooses to replace the event, later processors will react to the replacement instead.
	  * @return The final accumulated reaction which includes cancellation, replacement, and append information.
	  */
	private def process(event: Event, procs: List[EventProcessor])(implicit exc: ExecutionContext): EventProcessorReaction = {
		// get an eventreaction from the given processor for the current `event`
		def getReactionFrom(proc: EventProcessor) = proc.process.lift(event) getOrElse EventReactions()

		// get the replacement from the given reaction, or else the current `event`
		def replacement(r: EventReactions) = r.replacement getOrElse event

		procs match {
			// recursive end case: no more processors, so return no reaction
			case Nil => EventReactions()

			// get the reaction from `p`, then recurse on `procsTail`
			case p :: procsTail => getReactionFrom(p) match {
				// if the processor canceled the event, just return that reaction and stop recursing
				case r: EventReactions if r.isCanceled => r

				// for other reactions, take the reaction and add whatever recursion gives.
				// make sure to propagate the reaction's replacement where applicable
				case r: EventReactions => r ++ process(replacement(r), procsTail)

				// for future reactions, map the future, with the same inner behavior
				// as if the reactions were immediate reactions
				case EventReactionsFuture(rFuture) => EventReactionsFuture(rFuture flatMap {
					// on cancel, return immediately
					case r if r.isCanceled => Future.successful(r)

					// for others, recurse and add
					case r => { r ++ process(replacement(r), procsTail) } match {
						case r: EventReactions => Future.successful(r)
						case EventReactionsFuture(r) => r
					}
				})
			}
		}
	}
}