package io.dylemma.battle

object Priority {
	def apply(tiers: Int*): Priority = apply(tiers.toList)

	def main(args: Array[String]): Unit = {
		val pList = List(
			Priority(1),
			Priority(1, 100),
			Priority(1, 50),
			Priority(0),
			Priority(),
			Priority(1, 50),
			Priority(1, 50, 1),
			Priority(1, 50, 2),
			Priority(-1, 100),
			Priority(-1, 99),
			Priority(1) #> Priority(75, -1) #> Priority(10))
		pList.sorted foreach println
	}
}

case class Priority(tiers: List[Int]) extends Ordered[Priority] {
	def compare(that: Priority): Int = tierCompare(this.tiers, that.tiers)

	def #>(that: Priority) = Priority(this.tiers ++ that.tiers)

	private def tierCompare(thisTiers: List[Int], thatTiers: List[Int]): Int = (thisTiers, thatTiers) match {
		case (thisHead :: thisTail, thatHead :: thatTail) =>
			val headCmp = thatHead - thisHead
			if (headCmp == 0) tierCompare(thisTail, thatTail) // re-compare on the tails
			else headCmp // use the head comparison as long as it is nonzero
		case (thisHead :: thisTail, Nil) => -thisHead // this < that, meaning this is before that in priority
		case (Nil, thatHead :: thatTail) => thatHead // this > that, meaning this is after that in priority
		case (Nil, Nil) => 0 // this == that
	}
}