package io.dylemma.battle.old

class Resource(val max: Int) {
	private var _current = max
	def current = _current
	private def current_=(c: Int) = { _current = c }

	def restore(amount: Int): Unit = {
		current = math.min(max, current + amount)
	}
	def deplete(amount: Int): Unit = {
		current = math.max(0, current - amount)
	}

	def isFull = { current == max }
	def isEmpty = { current == 0 }
}

sealed abstract class ResourceKey(name: String) {
	override def toString = name
}
object ResourceKey {
	case object Mana extends ResourceKey("Mana")
	case object HP extends ResourceKey("HP")
	//etc.
}

trait HasResources {
	protected def resources: PartialFunction[ResourceKey, Resource]
	private val emptyResource = new Resource(0)

	def getResource(key: ResourceKey): Resource =
		resources lift key getOrElse emptyResource
}