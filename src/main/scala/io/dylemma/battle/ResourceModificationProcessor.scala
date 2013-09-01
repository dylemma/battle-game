package io.dylemma.battle

import scala.concurrent.ExecutionContext

class ResourceModificationProcessor extends EventProcessor {

	// this processor should always act last
	def priority = Integer.MIN_VALUE

	def process(implicit exc: ExecutionContext) = {

		// if a target is about to take damage, it takes damage
		case AboutToDamageResource(target, resource, damage, affiliation) =>
			DamagedResource(target, resource, damage, affiliation)

		// if a target is about to be restored, it is restored
		case AboutToRestoreResource(target, resource, amount, affiliation) =>
			RestoredResource(target, resource, amount, affiliation)

		// if a combattant took damage to a resource...
		case DamagedResource(t @ CombattantTarget(target), resKey, damage, affiliation) =>
			val resource = target getResource resKey
			val depleted = resource deplete damage.amount
			debug(s"$target lost $depleted $resKey")
			if (resource.isEmpty) TargetFainted(t)

		// if a combattant got a resource restored...
		case RestoredResource(CombattantTarget(target), resKey, amount, affiliation) =>
			val restored = target getResource resKey restore amount
			debug(s"$target gained $restored $resKey")

	}
}