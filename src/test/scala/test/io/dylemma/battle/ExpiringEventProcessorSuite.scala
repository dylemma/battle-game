package test.io.dylemma.battle

import io.dylemma.battle._
import concurrent.ExecutionContext.Implicits.global
import org.scalatest.FunSuite
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext

class ExpiringEventProcessorSuite extends FunSuite {
	def getHappenedEvents(queue: EventQueue, inputEvents: List[Event]): List[Event] = {
		val lb = List.newBuilder[Event]
		val cb = (e: Event) => Future.successful[Unit] { lb += e }
		val eventsFuture = queue.runEventQueue(inputEvents, cb) map { _ => lb.result }
		Await.result(eventsFuture, Duration.Inf)
	}

	case object Tick extends Event
	case object Tock extends Event

	test("A Processor that expires should generate an 'EventProcessorRemoved' event for itself") {
		val expirer = new ExpiringEventProcessor {
			val exp = new ExpirationTicker(2)
			def priority = 0
			def process(implicit exc: ExecutionContext) = {
				// exp increments to 1
				// append a Tock
				// exp increments to 2
				// expire
				case Tick => if (exp.tickAndCheck) expireMe else Tock
			}
		}
		val inputEvents = List(Tick, Tick, Tick)
		val queue = new EventQueue(expirer)
		val outputEvents = getHappenedEvents(queue, inputEvents)
		assert(outputEvents == List(Tick, Tock, Tick, EventProcessorRemoved(expirer), Tick))
	}

	test("A Processor may expire after its final reaction") {
		val expirer = new ExpiringEventProcessor {
			val exp = new ExpirationTicker(2)
			def priority = 0
			def process(implicit exc: ExecutionContext) = {
				// Tick => exp increments to 1; append a Tock
				// Tick => exp increments to 2; append a Tock
				// Tick => expire
				case Tick => if (exp.check) expireMe else { exp.tick; Tock }
			}
		}
		val inputEvents = List(Tick, Tick, Tick)
		val queue = new EventQueue(expirer)
		val outputEvents = getHappenedEvents(queue, inputEvents)
		assert(outputEvents == List(Tick, Tock, Tick, Tock, Tick, EventProcessorRemoved(expirer)))
	}
}