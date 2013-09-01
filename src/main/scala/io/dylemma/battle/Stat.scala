package io.dylemma.battle

class Stat(val baseAmount: Int) {
	private var bonuses: List[StatBonus] = Nil

	def addBonus(bonus: StatBonus): this.type = {
		bonuses = bonus :: bonuses sortBy { _.priority }
		this
	}
	def removeBonus(bonus: StatBonus): this.type = {
		bonuses = bonuses.filterNot(_ == bonus)
		this
	}

	def effectiveValue: Int = {
		val base = baseAmount.toDouble
		val effective = bonuses.foldLeft(base) { (amt, next) => next.transform(amt) }
		effective.toInt
	}
}

sealed abstract class StatKey(val name: String, val shortName: String) {
	override def toString = name
}

object StatKey {
	case object Strength extends StatKey("strength", "str")
	case object Agility extends StatKey("agility", "agi")
	case object Intelligence extends StatKey("intelligence", "int")
}

trait HasStats {
	protected def stats: PartialFunction[StatKey, Stat]
	private val emptyStat = new Stat(0)

	def getStat(key: StatKey) = stats lift key getOrElse emptyStat
}

trait StatBonus {
	def transform: Double => Double
	def priority: Int
}

case class AdditiveStatBonus(amount: Double, priority: Int) extends StatBonus {
	def transform = _ + amount
}

case class MultiplicativeStatBonus(factor: Double, priority: Int) extends StatBonus {
	def transform = _ * factor
}