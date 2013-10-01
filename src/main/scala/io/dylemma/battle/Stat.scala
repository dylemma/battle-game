package io.dylemma.battle

abstract class StatKey(val name: String, val shortName: String) {
	override def toString = name
}

object StatKey {
	case object Strength extends StatKey("strength", "str")
	case object Agility extends StatKey("agility", "agi")
	case object Intelligence extends StatKey("intelligence", "int")
	case object Speed extends StatKey("speed", "spd")
}

trait HasStats {
	protected def stats: PartialFunction[StatKey, Int]

	def getStat(key: StatKey) = stats lift key getOrElse 0
}