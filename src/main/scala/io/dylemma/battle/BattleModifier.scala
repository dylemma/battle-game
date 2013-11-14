package io.dylemma.battle

import scala.collection.GenTraversableOnce
import scala.collection.mutable.{ HashSet, SetProxy }
import scala.collection.SetLike

trait BattleModifier

class BattleModifiers(protected val modifiers: Set[BattleModifier]) extends Set[BattleModifier] with SetLike[BattleModifier, BattleModifiers] {

	def +(mod: BattleModifier) = new BattleModifiers(modifiers + mod)
	def -(mod: BattleModifier) = new BattleModifiers(modifiers - mod)
	def contains(mod: BattleModifier) = modifiers contains mod
	override def empty = BattleModifiers.empty
	def iterator = modifiers.iterator
}

object BattleModifiers {
	val empty = new BattleModifiers(Set.empty)
	def apply(mods: BattleModifier*) = new BattleModifiers(mods.toSet)
}
