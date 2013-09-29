package test.io.dylemma.battle

import io.dylemma.battle._
import org.scalatest.FunSuite

class EventQueueSuite extends FunSuite with EventProcessorHelpers {

	//	def collectHappenedEvents(queue: EventProcessor2, inputEvents: List[Event]): List[Event] = {
	//		val lb = List.newBuilder[Event]
	//		val cb: Event => Unit = lb += _
	//		queue.withEventCallback(cb) {
	//			queue.processAll(inputEvents: _*)
	//		}
	//		lb.result
	//	}
	//
	//	def eventQueue(handlers: EventHandler*): EventProcessor2 = {
	//		EventProcessor2(handlers.toSet, BattleModifiers.empty)
	//	}

	case object EventA extends Event with UnprioritizedEvent
	case object EventB extends Event with UnprioritizedEvent
	case object EventC extends Event with UnprioritizedEvent

	test("An EventQueue with no processors will cause each inputEvent to happen") {
		val queue = eventProcessor()
		val inputEvents = List(EventA, EventB, EventC)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(events == inputEvents)
	}

	test("An EventQueue will not allow an inputEvent to 'happen' if a processor cancels it") {
		val cancellor = new EventHandler {
			def priority = Priority(0)
			def handlePreEvent(mods: BattleModifiers) = {
				case EventA => Some(CancelEvent)
			}
			def handlePostEvent(mods: BattleModifiers) = PartialFunction.empty
		}
		val queue = eventProcessor(cancellor)
		val inputEvents = List(EventA, EventB)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(events == List(EventB))
	}

	test("An EventQueue will not send a cancelled event to any further processors") {
		val cancellor = new EventHandler {
			def priority = Priority(1)
			override def toString = "EventProcessor(cancel EventA)"
			def handlePreEvent(mods: BattleModifiers) = {
				case EventA => Some(CancelEvent)
			}
			def handlePostEvent(mods: BattleModifiers) = PartialFunction.empty
		}

		var seenEvents = 0
		val seer = new EventHandler {
			def priority = Priority(0)
			override def toString = "EventProcessor(increment counter)"
			def handlePostEvent(mods: BattleModifiers) = {
				case event =>
					seenEvents += 1
					Nil
			}
			def handlePreEvent(mods: BattleModifiers) = PartialFunction.empty
		}

		val queue = eventProcessor(cancellor, seer)
		val inputEvents = List(EventA, EventB, EventC)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(events == List(EventB, EventC) && seenEvents == 2)
	}

	test("An EventQueue handles replacement reactions") {
		val replacer = new EventHandler {
			def priority = Priority(0)
			def handlePreEvent(mods: BattleModifiers) = {
				case EventC => Some(ReplaceEvent(EventB))
			}
			def handlePostEvent(mods: BattleModifiers) = PartialFunction.empty
		}
		val queue = eventProcessor(replacer)
		val inputEvents = List(EventA, EventB, EventC)
		val events = collectHappenedEvents(queue, inputEvents)
		assert(events == List(EventA, EventB, EventB))
	}

	test("An EventQueue handles append reactions") {
		val appender = new EventHandler {
			def priority = Priority(0)
			def handlePreEvent(mods: BattleModifiers) = PartialFunction.empty
			def handlePostEvent(mods: BattleModifiers) = {
				case EventA => List(NewEvent(EventB), NewEvent(EventC))
			}
		}
		val queue = eventProcessor(appender)
		val events = collectHappenedEvents(queue, List(EventA))
		assert(events == List(EventA, EventB, EventC))
	}
}