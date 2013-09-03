package io.dylemma.battle

import scala.collection.immutable.SortedSet

/** A BattleModifier that affects a single stat value of some target.
  *
  * @param target The affected entity
  * @param key The key to the affected stat
  * @param priority A number indicating the ordering of the modifier. Generally,
  * higher priority-valued modifiers will be called first when calculating the
  * modified value of a stat.
  * @param mod A function that transforms a stat value into a new stat value
  */
case class StatModifier(target: HasStats, key: StatKey, priority: Int, mod: Double => Double) extends BattleModifier

object StatModifier {
	def additive(amount: Double, priority: Int)(target: HasStats, key: StatKey) = StatModifier(target, key, priority, _ + amount)
	def multiplicative(factor: Double, priority: Int)(target: HasStats, key: StatKey) = StatModifier(target, key, priority, _ * factor)
}

/** Mixin for an object that contains a set of `BattleModifier`s, providing some convenience methods
  * that calculate effective stat values based on the internal modifier set. Classes that want to mix
  * in this trait must define `val modifiers`. It is expected that the `modifiers` value will never
  * change.
  */
trait HasStatModifiers {
	val modifiers: Set[BattleModifier]

	/** Orders StatModifiers descending by priority: higher priority comes earlier in the ordering */
	implicit val statModifierOrdering: Ordering[StatModifier] = Ordering.by { -_.priority }

	/** Get a SortedSet containing the StatModifiers that affect the given `target` and `key`.
	  * @param target Something that has stats
	  * @param key A StatKey used to look up a stat value from the target
	  * @return A set of StatModifiers which affect the given `target` and `key`. The set is ordered
	  * such that the highest priority modifiers will be traversed first.
	  */
	def getStatModifiers(target: HasStats, key: StatKey): SortedSet[StatModifier] = modifiers.iterator.collect {
		case m @ StatModifier(`target`, `key`, _, _) => m
	}.to[SortedSet]

	/** Gets a "projection" of the `target`, where all stats on the projection return values
	  * modified by all of the modifiers in this modifier set.
	  * @param target Something that has stats
	  * @return An object whose stat values reflect those of the `target` after being
	  * modified with all of the applicable modifiers in this modifier set.
	  */
	def getStatProjection(target: HasStats): HasStats = new HasStatsProjection(target)

	private class HasStatsProjection(target: HasStats) extends HasStats {
		private val statCache = collection.mutable.Map[StatKey, Int]()
		protected def stats = {
			case key => statCache.getOrElseUpdate(key, getEffectiveStat(target, key))
		}
	}

	/** Gets the effective stat value based on the modifiers in this modifier set.
	  * @param target The object to look up stats from
	  * @param key The key for the stat to look up
	  * @return The target's modified value of the given stat
	  */
	def getEffectiveStat(target: HasStats, key: StatKey): Int = {
		// get modifiers for the Target+Key as a SortedSet (order is important)
		val mods = getStatModifiers(target, key)

		// calculation is done with everything as Doubles to keep accuracy between mods
		val statDouble = mods.foldLeft[Double](target getStat key) { (stat, modifier) =>
			modifier.mod(stat)
		}

		// finally downcast to an Integer
		statDouble.toInt
	}

}