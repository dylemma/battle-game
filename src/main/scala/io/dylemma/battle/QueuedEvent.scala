package io.dylemma.battle

//trait QueuedEvent {
//	def event: Event
//	def cancel: this.type
//	def append(event: Event): this.type
//}

//class QueuedEvent(val event: Event) {
//	private var _canceled = false
//	private val _appended = List.newBuilder[Event]
//
//	def canceled = _canceled
//	def appended = _appended.result
//
//	def cancel = {
//		_canceled = true
//		this
//	}
//
//	def append(event: Event) = {
//		_appended += event
//		this
//	}
//}