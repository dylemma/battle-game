package io.dylemma.util

object AsInt {
	def unapply(string: String): Option[Int] = try {
		val i = Integer.valueOf(string)
		Some(i)
	} catch {
		case e: NumberFormatException => None
	}
}