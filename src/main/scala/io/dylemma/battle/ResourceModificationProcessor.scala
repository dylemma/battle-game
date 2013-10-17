package io.dylemma.battle

import scala.concurrent.ExecutionContext
import EventHandlerSyntax._

class ResourceModificationProcessor extends EventHandler {

	// this processor should always act last
	def priority = Priority(Integer.MIN_VALUE)

	def handlePreEvent(context: Battleground) = PartialFunction.empty

	def handlePostEvent(battleground: Battleground) = {
		case DamageResource(target, res, dmg) =>
			val resource = target getResource res
			println("damage: " + dmg)
			if (Damage.isCritical(dmg)) debug("A critical hit!")
			val depleted = resource deplete dmg.amount.toInt
			debug(s"$target lost $depleted $res")

			target match {
				case cmb: Combattant if resource.isEmpty =>
					debug(s"$cmb was downed!")
					reactions { CombattantDowned(cmb) }
				case _ => noReactions
			}

		case RestoreResource(target, res, amount) =>
			val resource = target getResource res
			val restored = resource restore amount
			debug(s"$target gained $restored $res")
			noReactions
	}
}

