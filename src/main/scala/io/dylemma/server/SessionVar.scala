package io.dylemma.server

trait SessionVar[T] {
	private val map = collection.mutable.Map[SessionId, T]()

	def get(session: SessionId) = map get session
	def apply(session: SessionId) = map(session)

	def update(session: SessionId, value: T) = map.update(session, value)
	def getOrElseUpdate(session: SessionId, value: => T) = map.getOrElseUpdate(session, value)
}