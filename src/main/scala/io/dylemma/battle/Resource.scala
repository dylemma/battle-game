package io.dylemma.battle

class Resource(val max: Int) {
	private var _current = max
	def current = _current
	private def current_=(c: Int) = { _current = c }

	/** Restore the resource by the amount, returning the
	  * actual amount that got restored (may be less in
	  * case it would have been restored beyond the max).
	  * @param amount The amount to restore
	  * @return The actual amount restored
	  */
	def restore(amount: Int): Int = {
		val start = current
		current = math.min(max, current + amount)
		current - start
	}

	/** Deplete the resource by the amount, returning the
	  * actual amount that got restored (may be less in
	  * case it would have been restored beyond the max).
	  * @param The amount to deplete
	  * @return The actual amount depleted
	  */
	def deplete(amount: Int): Int = {
		val start = current
		current = math.max(0, current - amount)
		start - current
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