package io.dylemma.battle

import collection.mutable.{
	Map => MuMap,
	SortedSet => MuSortedSet
}

abstract class StatKey(val name: String, val shortName: String) {
	override def toString = name
}

object StatKey {
	case object Strength extends StatKey("strength", "str")
	case object Agility extends StatKey("agility", "agi")
	case object Intelligence extends StatKey("intelligence", "int")
	case object Speed extends StatKey("speed", "spd")
}

trait StatModifier extends Ordered[StatModifier] {
	def affectedStat: StatKey
	def priority: Int
	def modify(statValue: Double): Double

	def compare(that: StatModifier) = {
		that.priority - this.priority
	}
}

object StatModifier {
	def apply(stat: StatKey, _priority: Int, mod: Double => Double): StatModifier = new StatModifier {
		def affectedStat = stat
		def priority = _priority
		def modify(statValue: Double) = mod(statValue)
	}

	def additive(amount: Double = 0, priority: Int = 0)(stat: StatKey) =
		StatModifier(stat, priority, { _ + amount })

	def multiplicative(factor: Double = 1, priority: Int = 0)(stat: StatKey) =
		StatModifier(stat, priority, { _ * factor })
}

/** Mixin that adds Stats and StatModifiers to a class. Implementing classes
  * just need to provide a `stats` map value.
  */
trait HasStats {

	/** A Map that contains the base value for each stat
	  * that this object has.
	  */
	protected val stats: Map[StatKey, Int]

	/** Keeps track of stat modifiers on a per-stat basis */
	private val statModifiers = MuMap[StatKey, MuSortedSet[StatModifier]]()

	/** Get the unmodified value of the given `stat`
	  * @param stat A StatKey used to look up the value of the stat
	  * @return The integer value that this object has for the `stat`
	  */
	def getBaseStat(stat: StatKey) = stats.getOrElse(stat, 0)

	/** Get the value of the given `stat` after it has been modified
	  * by any StatModifiers on this object.
	  * @param stat A StatKey used to look up the value of the stat
	  * @return The integer value, after modifications, that this
	  * object has for the `stat`.
	  */
	def getEffectiveStat(stat: StatKey): Int = {
		// get the modifiers for the `stat`
		val mods = statModifiers.get(stat).map(_.toList).getOrElse(Nil)

		// fold in the modifications (as Double) starting from the base stat
		val d = mods.foldLeft[Double](getBaseStat(stat)) { (s, mod) =>
			mod.modify(s)
		}

		// downcast the computation to an Integer
		d.toInt
	}

	def addModifier(mod: StatModifier): this.type = {
		val set = statModifiers.getOrElseUpdate(mod.affectedStat, MuSortedSet())
		set += mod
		this
	}

	def removeModifier(mod: StatModifier): this.type = {
		for { set <- statModifiers.get(mod.affectedStat) } set -= mod
		this
	}

	def getModifiers = for {
		(key, set) <- statModifiers
		mod <- set
	} yield mod
}

