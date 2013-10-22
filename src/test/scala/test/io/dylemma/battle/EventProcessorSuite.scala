package test.io.dylemma.battle

import io.dylemma.battle._
import org.scalatest.FunSuite
import EventHandlerSyntax._

class EventProcessorSuite extends FunSuite with EventProcessorHelpers {

	case object EventA extends Event with UnprioritizedEvent
	case object EventB extends Event with UnprioritizedEvent
	case object EventC extends Event with UnprioritizedEvent

	implicit val myExecutionContext = SyncExecutionContext

	test("An EventProcessor with no processors will cause each inputEvent to happen") {
		val queue = eventProcessor()
		val inputEvents = List(EventA, EventB, EventC)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(waitFor(events) == inputEvents)
	}

	test("An EventProcessor will not allow an inputEvent to 'happen' if a processor cancels it") {
		val cancellor = new SyncEventHandler {
			def priority = Priority(0)
			def handlePreEvent(battleground: Battleground) = {
				case EventA => reactions { CancelEvent }
			}
			def handlePostEvent(battleground: Battleground) = PartialFunction.empty
		}
		val queue = eventProcessor(cancellor)
		val inputEvents = List(EventA, EventB)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(waitFor(events) == List(EventB))
	}

	test("An EventProcessor will not send a cancelled event to any further processors") {
		val cancellor = new SyncEventHandler {
			def priority = Priority(1)
			override def toString = "EventProcessor(cancel EventA)"
			def handlePreEvent(battleground: Battleground) = {
				case EventA => reactions { CancelEvent }
			}
			def handlePostEvent(battleground: Battleground) = PartialFunction.empty
		}

		var seenEvents = 0
		val seer = new SyncEventHandler {
			def priority = Priority(0)
			override def toString = "EventProcessor(increment counter)"
			def handlePostEvent(battleground: Battleground) = {
				case event =>
					seenEvents += 1
					noReactions
			}
			def handlePreEvent(battleground: Battleground) = PartialFunction.empty
		}

		val queue = eventProcessor(cancellor, seer)
		val inputEvents = List(EventA, EventB, EventC)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(waitFor(events) == List(EventB, EventC) && seenEvents == 2)
	}

	test("An EventProcessor handles replacement reactions") {
		val replacer = new SyncEventHandler {
			def priority = Priority(0)
			def handlePreEvent(battleground: Battleground) = {
				case EventC => reactions { ReplaceEvent(EventB) }
			}
			def handlePostEvent(battleground: Battleground) = PartialFunction.empty
		}
		val queue = eventProcessor(replacer)
		val inputEvents = List(EventA, EventB, EventC)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(waitFor(events) == List(EventA, EventB, EventB))
	}

	test("An EventProcessor handles append reactions") {
		val appender = new SyncEventHandler {
			def priority = Priority(0)
			def handlePreEvent(battleground: Battleground) = PartialFunction.empty
			def handlePostEvent(battleground: Battleground) = {
				case EventA => reactions { List(EventB, EventC) }
			}
		}
		val queue = eventProcessor(appender)
		val events = collectHappenedEvents(queue, List(EventA))
		assert(waitFor(events) == List(EventA, EventB, EventC))
	}
}