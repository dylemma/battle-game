package io.dylemma.battle

import scala.util.Random
import TargetHelpers._
import Damage._

object DamageFormula {

	type DamageCalculation = (Combattant, Target, BattleModifiers) => Damage
	type DamageModification = (Combattant, Target, BattleModifiers) => (Damage => Damage)

	/** Allows chaining of DamageModifications and Decorators on a DamageCalculation via the `~` method */
	implicit class DamageCalculationCombiner(calculation: DamageCalculation) {
		def ~(modifier: DamageModification): DamageCalculation = {
			(a, d, m) =>
				val calc = calculation(a, d, m)
				val mod = modifier(a, d, m)
				mod(calc)
		}
		def ~(decorator: Damage.Decorator): DamageCalculation = {
			(a, d, m) =>
				val calc = calculation(a, d, m)
				decorator.decorate(calc)
		}
	}

	private def levelModifier(level: Int): Double = { (level * 0.4) + 2 }
	private def defenseModifier(attack: Double, defense: Double): Double = { attack * 0.5 / (defense + 1) }
	private def powerMultiplier = 0.5

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
		case (attacker, defender, mods) =>
			val dmgOpt = for (defStats <- defender.project[HasStats]) yield {
				val attack = mods.getEffectiveStat(attacker, attackStat)
				val defense = mods.getEffectiveStat(defStats, defenseStat)
				val amount = basePower * powerMultiplier * levelModifier(attacker.level) * defenseModifier(attack, defense)
				Damage(amount)
			}
			dmgOpt getOrElse Damage(0)
	}

	/** Damage Modification that randomly a critical-hit multipler based on the `baseChance` probability.
	  * @param baseChance A number from 0 to 1 that represents the percentage chance of getting a crit
	  * @param mult The damage multiplier upon getting a crit
	  * @return The damage multiplier which will be 1 if no crit, or `mult` if crit.
	  */
	def criticalMultiplier(baseChance: Double, mult: Double): DamageModification = {
		/*
		 * Currently the inputs have no effect on the crit chance, but in the future,
		 * some modifiers could potentially alter the chance or multiplier.
		 */
		case (attacker, defender, mods) => originalDamage =>

			val c = Random.nextDouble // [0.0 ... 1.0]
			if (c <= baseChance) originalDamage x Critical(mult)
			else originalDamage
	}

	/** Damage Modification that adds a random multiplier between 85% and 100%
	  * @return A damage multiplier in between 85% and 100% inclusive.
	  */
	def randomChanceMultiplier: DamageModification = {
		case (attacker, defender, mods) => originalDamage =>
			val minus = Random.nextInt(16) // returns one of [0..15]
			val mult = (100 - minus) * 0.01
			originalDamage x Multiplier(mult)
	}

	/** Damage Modification that attributes the damage to the attacker */
	def attackerSource: DamageModification = {
		case (attacker, defender, mods) => originalDamage =>
			originalDamage x Source(CombattantTarget(attacker))
	}
}