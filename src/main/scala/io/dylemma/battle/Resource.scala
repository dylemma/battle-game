package io.dylemma.battle

class Resource(val max: Int) extends Mutable {
	private var _current = max
	def current = _current
	private def current_=(c: Int) = { _current = c }

	override def toString = s"Resource($current of $max)"

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

	def copy: Resource = {
		val r = new Resource(max)
		r.current = current
		r
	}
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

	def projection = new HasResourcesProjection(this)
}

/** Lazy projection of an existing `target` HasResources instance. For each
  * resource that is looked up, the first time that key is looked up, it loads
  * the target's resource for that key and creates a copy of it. Modifications
  * to the copy will not affect the original resource.
  */
class HasResourcesProjection(target: HasResources) extends HasResources {
	private val rmap = collection.mutable.Map[ResourceKey, Resource]()
	protected def resources = {
		case key => rmap.get(key) match {
			case Some(res) => res
			case None => {
				val res = (target getResource key).copy
				rmap.put(key, res)
				res
			}
		}
	}
}