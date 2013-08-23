package io.dylemma.battle.old

trait BattleEffect {
	def affect(target: Combattant): Unit
}

case class DamageEffect(amount: Int) extends BattleEffect {
	def affect(target: Combattant) = target.hp deplete amount
}
case class HealingEffect(amount: Int) extends BattleEffect {
	def affect(target: Combattant) = target.hp restore amount
}