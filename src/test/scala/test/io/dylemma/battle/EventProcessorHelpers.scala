package test.io.dylemma.battle

import io.dylemma.battle._
import io.dylemma.util.BidiMap

trait EventProcessorHelpers {
	def collectHappenedEvents(processor: EventProcessor, inputEvents: List[Event]): List[Event] = {
		val lb = List.newBuilder[Event]
		val cb: Event => Unit = lb += _
		processor.withEventCallback(cb) {
			processor.processAll(inputEvents: _*)
		}
		lb.result
	}

	def eventProcessor(handlers: EventHandler*): EventProcessor = {
		EventProcessor(handlers.toSet, Battleground(BidiMap(), BattleModifiers.empty))
	}
}