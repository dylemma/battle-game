package test.io.dylemma.util

import org.scalatest.FunSuite
import io.dylemma.util.BidiMap

class BidiMapSuite extends FunSuite {

	test("positive lookup of right value from left key") {
		val map = BidiMap(1 -> "a")
		assert(map.fromLeft(1) == Some("a"))
	}

	test("negative lookup of right value from left key") {
		val map = BidiMap(1 -> "a")
		assert(map.fromLeft(2) == None)
	}

	test("positive lookup of left value from right key") {
		val map = BidiMap(1 -> "a")
		assert(map.fromRight("a") == Some(1))
	}

	test("negative lookup of left value from right key") {
		val map = BidiMap(1 -> "a")
		assert(map.fromRight("b") == None)
	}

	test("exclude duplicate mappings") {
		val map = BidiMap(1 -> "a", 1 -> "b")
		assert(map.size == 1 && map.entries.head == (1, "a"))
	}

	test("create from many pairs") {
		val pairs = List(1 -> "a", 2 -> "b", 3 -> "c")
		val map = BidiMap(pairs: _*)
		assert(map.size == 3 && map.entries.toList == pairs)
	}

}