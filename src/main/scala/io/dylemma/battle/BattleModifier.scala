package io.dylemma.battle

import scala.collection.immutable.SortedSet
import scala.collection.GenTraversableOnce

trait BattleModifier

class BattleModifiers(val modifiers: Set[BattleModifier]) extends HasStatModifiers {
	def this(modifiers: BattleModifier*) = this(modifiers.toSet)
	def this(modifiers: List[BattleModifier]) = this(modifiers.toSet)

	def +(mod: BattleModifier) = new BattleModifiers(modifiers + mod)
	def -(mod: BattleModifier) = new BattleModifiers(modifiers - mod)
	def ++(mods: GenTraversableOnce[BattleModifier]) = new BattleModifiers(modifiers ++ mods)
	def --(mods: GenTraversableOnce[BattleModifier]) = new BattleModifiers(modifiers -- mods)
}
