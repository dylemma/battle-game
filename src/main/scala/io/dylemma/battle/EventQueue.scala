package io.dylemma.battle

import scala.annotation.tailrec
import scala.collection.mutable.Queue
import scala.collection.mutable.SortedSet

class EventQueue(initialProcessors: EventProcessor*) {
	private val pp = SortedSet[EventProcessor](initialProcessors: _*)

	def enter(event: Event) = {
		trace("Enter Event:", event)
		val qq = new Queue[Event]
		qq enqueue event

		@tailrec def runEvents: Unit = qq.dequeueFirst { _ => true } match {

			// Queue is empty
			case None => // done

			// Special Case:
			// ignore events that would remove a processor that doesn't exist
			case Some(EventProcessorRemoved(p)) if !(pp contains p) => runEvents

			// Normal Operation:
			// send event through processors for "cancel" and "append" ops
			case Some(event) => {
				val appends = List.newBuilder[Event]
				var canceled = false

				for (p <- pp.takeWhile(_ => !canceled)) {
					val qe = new QueuedEventImpl(event)
					trace(p, "process event:", event)
					p.process(qe)
					if (qe.canceled) trace(p, "Canceled", event)
					for (a <- qe.appended) trace(p, "Appended", a)

					appends ++= qe.appended
					canceled |= qe.canceled
				}

				qq.enqueue(appends.result: _*)
				if (!canceled) eventHappened(event)

				runEvents
			}
		}

		runEvents
	}

	def eventHappened(event: Event) = {
		debug("Event Happened:", event)
		event match {
			case EventProcessorAdded(p) => pp += p
			case EventProcessorRemoved(p) => pp -= p
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
		def process(qe: QueuedEvent) = qe.event match {
			case EventA => qe.append(EventAReaction)
			case _ =>
		}
	}

	val eventBProcessor = new EventProcessor {
		override def toString = "eventBProcessor"
		val priority = 0
		def process(qe: QueuedEvent) = qe.event match {
			case EventB => qe.append(EventProcessorAdded(eventAReactor))
			case _ =>
		}
	}

	case object EventCReplacement extends Event

	val eventCProcessor = new EventProcessor {
		override def toString = "eventCProcessor"
		val priority = 0
		def process(qe: QueuedEvent) = qe.event match {
			case EventC => qe.cancel.append(EventCReplacement)
			case _ =>
		}
	}

	def main(args: Array[String]): Unit = {
		val eq = new EventQueue(eventBProcessor, eventCProcessor)
		eq.enter(TurnBegan)
		eq.enter(EventA)
		eq.enter(EventB)
		eq.enter(EventA)
		eq.enter(EventC)
		eq.enter(TurnEnded)
	}
}