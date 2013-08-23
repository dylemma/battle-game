package io.dylemma.battle

trait Event

case object TurnBegan extends Event
case object TurnEnded extends Event
case class EventProcessorAdded(p: EventProcessor) extends Event
case class EventProcessorRemoved(p: EventProcessor) extends Event