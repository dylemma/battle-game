package io.dylemma.util

import scala.collection.mutable.HashMap
import scala.util.hashing.MurmurHash3

trait BidiMap[L, R] {
	def fromLeft(left: L): Option[R]
	def fromRight(right: R): Option[L]
	def entries: Iterable[(L, R)]
	def size: Int

	override def toString = entries.mkString("BidiMap(", ", ", ")")

	override def equals(that: Any) = that match {
		case thatMap: BidiMap[_, _] =>
			entries.toSet == thatMap.entries.toSet
		case _ => false
	}

	override def hashCode = MurmurHash3.unorderedHash(entries)
}

object BidiMap {
	def apply[L, R](entries: (L, R)*): BidiMap[L, R] = new BidiMapImpl(entries: _*)

	private class BidiMapImpl[L, R](unsafeEntries: (L, R)*) extends BidiMap[L, R] {
		import BidiMap._

		private val (entriesArray, leftIndexMap, rightIndexMap) = {
			val m = Array.newBuilder[(L, R)]
			var i = 0
			val lmap = new HashMap[L, Int]
			val rmap = new HashMap[R, Int]

			for {
				(l, r) <- unsafeEntries
				if !lmap.contains(l) && !rmap.contains(r)
			} {
				val e = l -> r
				m += e
				lmap.put(l, i)
				rmap.put(r, i)
				i += 1
			}

			(m.result, lmap.toMap, rmap.toMap)
		}

		def fromLeft(left: L): Option[R] = leftIndexMap.get(left).map { entriesArray(_)._2 }
		def fromRight(right: R): Option[L] = rightIndexMap.get(right).map { entriesArray(_)._1 }
		val entries: Iterable[(L, R)] = new Iterable[(L, R)] {
			def iterator = entriesArray.iterator
		}
		def size = entriesArray.length
	}
}
