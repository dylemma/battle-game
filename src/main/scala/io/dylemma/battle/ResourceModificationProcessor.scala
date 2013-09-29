package io.dylemma.battle

import scala.concurrent.ExecutionContext

class ResourceModificationProcessor extends EventHandler {

	// this processor should always act last
	def priority = Priority(Integer.MIN_VALUE)

	def handlePreEvent(mods: BattleModifiers) = PartialFunction.empty

	def handlePostEvent(mods: BattleModifiers) = {
		case DamageResource(target, res, dmg) =>
			val resource = target getResource res
			val depleted = resource deplete dmg.amount
			debug(s"$target lost $depleted $res")
			Nil // TODO - death conditions?

		case RestoreResource(target, res, amount) =>
			val resource = target getResource res
			val restored = resource restore amount
			debug(s"$target gained $restored $res")
			Nil
	}

}