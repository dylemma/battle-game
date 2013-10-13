package test.io.dylemma.battle

import io.dylemma.battle._
import concurrent.ExecutionContext.Implicits.global
import org.scalatest.FunSuite
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

class ExpiringEventProcessorSuite extends FunSuite with EventProcessorHelpers {

	case object Tick extends Event with UnprioritizedEvent
	case object Tock extends Event with UnprioritizedEvent

	test("A Processor that expires should generate an 'EventProcessorRemoved' event for itself") {
		val expirer = new EventHandler with ExpirationHelpers {
			val exp = new ExpirationCounter(2)
			def priority = Priority(0)
			def handlePreEvent(battleground: Battleground) = PartialFunction.empty
			def handlePostEvent(battleground: Battleground) = {
				// exp increments to 1
				// append a Tock
				// exp increments to 2
				// expire
				case Tick =>
					if (exp.incrementAndCheck) expire(this)
					else List(NewEvent(Tock))
			}
		}
		val inputEvents = List(Tick, Tick, Tick)
		val queue = eventProcessor(expirer)
		val outputEvents = collectHappenedEvents(queue, inputEvents)
		assert(outputEvents == List(Tick, Tock, Tick, RemoveEventHandler(expirer), Tick))
	}

	test("A Processor may expire after its final reaction") {
		val expirer = new EventHandler with ExpirationHelpers {
			val exp = new ExpirationCounter(2)
			def priority = Priority(0)
			def handlePreEvent(battleground: Battleground) = PartialFunction.empty
			def handlePostEvent(battleground: Battleground) = {
				// Tick => exp increments to 1; append a Tock
				// Tick => exp increments to 2; append a Tock
				// Tick => expire
				case Tick => if (exp.check) expire(this) else {
					exp.increment
					List(NewEvent(Tock))
				}
			}
		}
		val inputEvents = List(Tick, Tick, Tick)
		val queue = eventProcessor(expirer)
		val outputEvents = collectHappenedEvents(queue, inputEvents)
		assert(outputEvents == List(Tick, Tock, Tick, Tock, Tick, RemoveEventHandler(expirer)))
	}
}