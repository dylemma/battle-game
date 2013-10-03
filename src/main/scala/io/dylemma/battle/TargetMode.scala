package io.dylemma.battle

import TargetHelpers._

trait TargetMode {
	def canTarget(user: Combattant, target: Target, mods: BattleModifiers): Boolean
}

object TargetMode {

	object AnyTarget extends TargetMode {
		def canTarget(user: Combattant, target: Target, mods: BattleModifiers) = true
	}

	object OnlySelf extends TargetMode {
		def canTarget(user: Combattant, target: Target, mods: BattleModifiers) = {
			target == CombattantTarget(user)
		}
	}

	sealed abstract class CombattantAffiliationTarget(affiliation: Affiliation) extends TargetMode {
		def canTarget(user: Combattant, target: Target, mods: BattleModifiers) = {
			val affil = for {
				targetC <- target.projectAs[Combattant]
				affil <- mods.getAffiliation(user, targetC)
			} yield affil

			affil == Some(affiliation)
		}
	}

	object OnlyFriendly extends CombattantAffiliationTarget(Affiliation.Friendly)
	object OnlyHostile extends CombattantAffiliationTarget(Affiliation.Hostile)
	object OnlyNeutral extends CombattantAffiliationTarget(Affiliation.Neutral)

	object OnlyEnvironment extends TargetMode {
		def canTarget(user: Combattant, target: Target, mods: BattleModifiers) = target == EnvironmentTarget
	}
}