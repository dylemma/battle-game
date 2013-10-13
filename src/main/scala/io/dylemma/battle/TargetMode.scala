package io.dylemma.battle

import TargetHelpers._

trait TargetMode {
	def canTarget(user: Combattant, target: Target, battleground: Battleground): Boolean
}

object TargetMode {

	/** Accepts any target */
	object AnyTarget extends TargetMode {
		def canTarget(user: Combattant, target: Target, battleground: Battleground) = true
	}

	/** Only accepts the user as the target */
	object OnlySelf extends TargetMode {
		def canTarget(user: Combattant, target: Target, battleground: Battleground) = {
			target == CombattantTarget(user)
		}
	}

	/** Only accepts the `EnvironmentTarget` */
	object OnlyEnvironment extends TargetMode {
		def canTarget(user: Combattant, target: Target, battleground: Battleground) = target == EnvironmentTarget
	}

	/** Accepts any target that is a Combattant */
	object AnyCombattant extends TargetMode {
		def canTarget(user: Combattant, target: Target, battleground: Battleground) = target match {
			case CombattantTarget(_) => true
			case _ => false
		}
	}

	/** Target Mode based on a comparison of the user and target's positions
	  * along a given `axis`. If the either the user or the  target do not
	  * have a value on the given `axis`, the `canTarget` function will return
	  * `false`. As long as both values are available, `canTarget` will return
	  * the result of `checkPositions` with those values.
	  */
	abstract class SingleAxisPositionCheck[T](axis: Axis[T]) extends TargetMode {
		def checkPositions(userPos: T, targetPos: T): Boolean

		def canTarget(user: Combattant, target: Target, battleground: Battleground) = {
			val checkOpt = for {
				userPos <- battleground positionOf user
				userValue <- userPos getValue axis
				targetPos <- battleground positionOf target
				targetValue <- targetPos getValue axis
			} yield checkPositions(userValue, targetValue)

			checkOpt getOrElse false
		}
	}

	/** Accepts targets who are in a different party from the user, as long
	  * as the user and target are both in parties.
	  */
	object OpposingParty extends SingleAxisPositionCheck(PartyAxis) {
		def checkPositions(userParty: Party, targetParty: Party) = userParty != targetParty
	}

	/** Accepts targets who are in the same party as the user, as long
	  * as the user and target are both in parties.
	  */
	object SameParty extends SingleAxisPositionCheck(PartyAxis) {
		def checkPositions(userParty: Party, targetParty: Party) = userParty == targetParty
	}

}