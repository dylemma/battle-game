package io.dylemma.battle

trait SkillCost {
	protected def amountToSpend(on: HasResourcesProjection): List[(Int, ResourceKey)]
	def +(that: SkillCost): SkillCost = SkillCost.MultipleSkillCosts(this, that)
	def spend(on: HasResources): Unit = {
		val p = on.projection
		for { (amt, key) <- amountToSpend(p) } on.getResource(key).deplete(amt)
	}
	def canSpend(on: HasResources): Boolean = {
		val p = on.projection
		val costs = amountToSpend(p)
		costs.foldLeft(true) {
			case (can, (amt, key)) =>
				val r = p.getResource(key)
				can && (r.current >= amt)
		}
	}
}

object SkillCost {
	case object NoCost extends SkillCost {
		protected def amountToSpend(on: HasResourcesProjection) = Nil
		override def toString = "[NoCost]"
	}
	case class FlatResourceSkillCost(amount: Int, resource: ResourceKey) extends SkillCost {
		protected def amountToSpend(on: HasResourcesProjection) = List(amount -> resource)
		override def toString = s"[$amount $resource]"
	}
	case class PercentRelativeResourceSkillCost(percentage: Int, relativeTo: RelativeResourceAmount, resource: ResourceKey) extends SkillCost {
		protected def amountToSpend(on: HasResourcesProjection) = {
			val r = on.getResource(resource)
			val amount = relativeTo.getAmount(r) * percentage / 100
			List(amount -> resource)
		}
		override def toString = s"[$percentage% $relativeTo $resource]"
	}
	case class MultipleSkillCosts(head: SkillCost, tail: SkillCost) extends SkillCost {
		protected def amountToSpend(on: HasResourcesProjection) = {
			val headSpending = head.amountToSpend(on)
			head.spend(on)
			val tailSpending = tail.amountToSpend(on)
			headSpending ++ tailSpending
		}
		override def toString = s"$head + $tail"
	}

	trait RelativeResourceAmount {
		def getAmount(resource: Resource): Int
	}
	case object CurrentResourceAmount extends RelativeResourceAmount {
		def getAmount(resource: Resource) = resource.current
		override def toString = "current"
	}
	case object MaxResourceAmount extends RelativeResourceAmount {
		def getAmount(resource: Resource) = resource.max
		override def toString = "max"
	}
}

/** DSL for creating skill costs. Individual costs can be created with the `cost` method,
  * as well as the `noCost` method. Examples: {{{
  * val c1 = cost(10, HP) // 10 HP
  * val c2 = cost(5 % max(Mana)) // 5 percent of the user's max Mana
  * val c3 = cost(10 % current(HP)) // 10 percent of the user's current HP
  * }}}
  * Individual costs can still be appended to each other with the `+` operator.
  */
object SkillCostDSL {
	import SkillCost._

	def noCost: SkillCost = NoCost

	// e.g. cost(10, HP)
	def cost(amount: Int, resource: ResourceKey): SkillCost =
		FlatResourceSkillCost(amount, resource)

	// e.g. cost(5 % max(Mana))
	def cost(prra: PercentRelativeAmountTo): SkillCost =
		PercentRelativeResourceSkillCost(prra.percentage, prra.spec.rel, prra.spec.key)

	def current(resource: ResourceKey) = RelativeAmountTo(CurrentResourceAmount, resource)
	def max(resource: ResourceKey) = RelativeAmountTo(MaxResourceAmount, resource)

	// e.g. current(HP), max(Mana)
	case class RelativeAmountTo(rel: RelativeResourceAmount, key: ResourceKey)

	// e.g. 10 % current(HP)
	case class PercentRelativeAmountTo(percentage: Int, spec: RelativeAmountTo)
	implicit class IntAsPercentageSpecifier(percentage: Int) {
		def %(rel: RelativeAmountTo) = PercentRelativeAmountTo(percentage, rel)
	}
}