package test.io.dylemma.battle

import io.dylemma.battle._
import org.scalatest.FunSuite
import org.scalatest.concurrent.AsyncAssertions
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext

class EventQueueSuite extends FunSuite {

	def collectHappenedEvents(queue: EventQueue, inputEvents: List[Event]): Future[List[Event]] = {
		val lb = List.newBuilder[Event]
		val cb = (e: Event) => Future.successful[Unit] { lb += e }
		queue.runEventQueue(inputEvents, cb) map {
			case _ => lb.result
		}
	}

	case object EventA extends Event
	case object EventB extends Event
	case object EventC extends Event

	test("An EventQueue with no processors will cause each inputEvent to happen") {
		val queue = new EventQueue
		val inputEvents = List(EventA, EventB, EventC)
		val eventsFuture = collectHappenedEvents(queue, inputEvents)
		val events = Await.result(eventsFuture, Duration.Inf)
		assert(events == inputEvents)
	}

	test("An EventQueue will not allow an inputEvent to 'happen' if a processor cancels it") {
		val cancellor = new EventProcessor {
			def priority = 0
			def process(implicit exc: ExecutionContext) = {
				case EventA => reactions.cancel
			}
		}
		val queue = new EventQueue(cancellor)
		val inputEvents = List(EventA, EventB)
		val eventsFuture = collectHappenedEvents(queue, inputEvents)
		val events = Await.result(eventsFuture, Duration.Inf)
		assert(events == List(EventB))
	}

	test("An EventQueue will not send a cancelled event to any further processors") {
		val cancellor = new EventProcessor {
			def priority = 1
			override def toString = "EventProcessor(cancel EventA)"
			def process(implicit exc: ExecutionContext) = {
				case EventA => reactions.cancel
			}
		}

		var seenEvents = 0
		val seer = new EventProcessor {
			def priority = 0
			override def toString = "EventProcessor(increment counter)"
			def process(implicit exc: ExecutionContext) = {
				case event => seenEvents += 1
			}
		}

		val queue = new EventQueue(cancellor, seer)
		val inputEvents = List(EventA, EventB, EventC)
		val eventsFuture = collectHappenedEvents(queue, inputEvents)
		val events = Await.result(eventsFuture, Duration.Inf)
		assert(events == List(EventB, EventC) && seenEvents == 2)
	}

	test("An EventQueue handles replacement reactions") {
		val replacer = new EventProcessor {
			def priority = 0
			def process(implicit exc: ExecutionContext) = {
				case EventC => reactions.replaceWith(EventB)
			}
		}
		val queue = new EventQueue(replacer)
		val inputEvents = List(EventA, EventB, EventC)
		val eventsFuture = collectHappenedEvents(queue, inputEvents)
		val events = Await.result(eventsFuture, Duration.Inf)
		assert(events == List(EventA, EventB, EventB))
	}

	test("An EventQueue handles append reactions") {
		val appender = new EventProcessor {
			def priority = 0
			def process(implicit exc: ExecutionContext) = {
				case EventA => reactions + EventB + EventC
			}
		}
		val queue = new EventQueue(appender)
		val eventsFuture = collectHappenedEvents(queue, List(EventA))
		val events = Await.result(eventsFuture, Duration.Inf)
		assert(events == List(EventA, EventB, EventC))
	}
}