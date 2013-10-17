package test.io.dylemma.battle

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import io.dylemma.battle._
import EventHandlerSyntax._
import io.dylemma.util.BidiMap
import scala.concurrent.Await
import scala.concurrent.duration._

trait EventProcessorHelpers {
	def collectHappenedEvents(processor: EventProcessor, inputEvents: List[Event])(implicit exc: ExecutionContext): Future[List[Event]] = {
		val lb = List.newBuilder[Event]
		val handler = new EventHandler {
			def priority = Priority(0)
			def handlePreEvent(x: Battleground) = PartialFunction.empty
			def handlePostEvent(x: Battleground) = {
				case e =>
					lb += e
					noReactions
			}
		}
		val p = processor.copy(handlers = processor.handlers + handler)
		for {
			_ <- p.processAllFuture(inputEvents: _*)
		} yield lb.result
	}

	def waitFor[T](ft: Future[T], duration: Duration = 1.second): T = Await.result(ft, duration)

	def eventProcessor(handlers: EventHandler*): EventProcessor = {
		EventProcessor(handlers.toSet, Battleground(BidiMap(), BattleModifiers.empty))
	}
}