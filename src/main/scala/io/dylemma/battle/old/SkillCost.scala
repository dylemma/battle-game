package io.dylemma.battle.old

import io.dylemma.battle.ResourceKey

sealed trait SkillCost { self =>
	def canSpend(user: Combattant): Boolean
	def spend(user: Combattant): Unit
	def +(that: SkillCost): SkillCost
}

object SkillCost {
	def flatten(costs: List[SkillCost]): List[SkillCost] = {
		val flatList = collection.mutable.ListBuffer[SkillCost]()
		def recurse(cost: SkillCost): Unit = cost match {
			case CompoundCost(costs) => costs.foreach(recurse)
			case c => flatList += c
		}
		costs.foreach(recurse)

		val fl = flatList.result
		val (rCosts, otherCosts) = fl partition {
			case c: ResourceCost => true
			case _ => false
		}

		val mergedResourceCosts = rCosts
			.map(_.asInstanceOf[ResourceCost])
			.groupBy(_.resourceKey)
			.mapValues(_.map(_.amount).sum)
			.map { case (key, amount) => ResourceCost(amount, key) }
			.toList

		otherCosts ++ mergedResourceCosts
	}
}

case object FreeCost extends SkillCost {
	def canSpend(user: Combattant) = true
	def spend(user: Combattant) = ()
	def +(that: SkillCost) = that

	override def toString = "No Cost"
}

case class ResourceCost(amount: Int, resourceKey: ResourceKey) extends SkillCost {
	def canSpend(user: Combattant): Boolean = user.getResource(resourceKey).current >= amount
	def spend(user: Combattant): Unit = user.getResource(resourceKey) deplete amount

	def +(that: SkillCost): SkillCost = that match {
		case FreeCost => this
		case ResourceCost(a, `resourceKey`) => ResourceCost(amount + a, resourceKey)
		case CompoundCost(lst) => CompoundCost(this :: lst)
		case that => CompoundCost(this :: that :: Nil)
	}

	override def toString = s"$amount ${resourceKey.toString}"
}

class CompoundCost private (val costs: List[SkillCost], flattened: Boolean) extends SkillCost {
	def this(costs: List[SkillCost]) = this(SkillCost.flatten(costs), true)

	def canSpend(user: Combattant): Boolean = costs forall { _ canSpend user }
	def spend(user: Combattant): Unit = costs foreach { _ spend user }

	def +(that: SkillCost): SkillCost = that match {
		case FreeCost => this
		case CompoundCost(lst) => CompoundCost(costs ++ lst)
		case that => CompoundCost(that :: costs)
	}

	override def toString = costs.mkString("[", " + ", "]")
}

object CompoundCost {
	def apply(costs: List[SkillCost]) = new CompoundCost(costs)
	def unapply(cc: CompoundCost): Option[List[SkillCost]] = Some(cc.costs)
}

object SkillCostDSL extends SkillCostDSL

trait SkillCostDSL {

	implicit class ResourceKeySkillCostOps(k: ResourceKey) {
		def cost(amount: Int) = ResourceCost(amount, k)
	}

	//example: (Mana cost 10) + (HP cost 20)
}