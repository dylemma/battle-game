package io.dylemma.battle

import scala.util.Random

object DamageFormula {

	type DamageCalculation = (Combattant, Target, BattleModifiers) => Double

	implicit class DamageCalculationCombiner(dc: DamageCalculation) {
		def *(that: DamageCalculation): DamageCalculation = {
			(a, d, m) => dc(a, d, m) * that(a, d, m)
		}
		def +(that: DamageCalculation): DamageCalculation = {
			(a, d, m) => dc(a, d, m) + that(a, d, m)
		}
	}

	private def levelModifier(level: Int): Double = { (level * 0.4) + 2 }
	private def defenseModifier(attack: Double, defense: Double): Double = { attack * 0.5 / (defense + 1) }

	/** Damage Calculation that generates a base damage amount based on the `basePower` along with
	  * the attack and defense stats of the attacker and defender, respectively. If the defender is
	  * not a Combattant, this calculation will simply return 0.
	  *
	  * @param basePower The base power of the damage
	  * @param attackStat The stat that dictates the attack power of the attacker
	  * @param defenseStat The stat that dictates the defense power of the defender
	  * @return A damage amount (0 or greater)
	  */
	def basicDamage(basePower: Int, attackStat: StatKey, defenseStat: StatKey): DamageCalculation = {
		case (attacker, CombattantTarget(defender), mods) =>
			val attack = mods.getEffectiveStat(attacker, attackStat)
			val defense = mods.getEffectiveStat(defender, defenseStat)
			basePower * levelModifier(attacker.level) * defenseModifier(attack, defense)
		case _ => 0
	}

	/** Damage Calculation that returns a critical-hit multipler.
	  * @param baseChance A number from 0 to 1 that represents the percentage chance of getting a crit
	  * @param mult The damage multiplier upon getting a crit
	  * @return The damage multiplier which will be 1 if no crit, or `mult` if crit.
	  */
	def criticalMultiplier(baseChance: Double, mult: Double): DamageCalculation = {
		/*
		 * Currently the inputs have no effect on the crit chance, but in the future,
		 * some modifiers could potentially alter the chance or multiplier.
		 */
		case _ =>
			val c = Random.nextDouble // [0.0 ... 1.0]
			if (c <= baseChance) mult
			else 1.0
	}

	/** Damage Calculation that returns a damage multiplier based on random chance.
	  * @return A damage multiplier in between 85% and 100% inclusive.
	  */
	def randomChanceMultiplier: DamageCalculation = {
		case _ =>
			val minus = Random.nextInt(16) // returns one of [0..15]
			(100 - minus) * 0.01
	}

}