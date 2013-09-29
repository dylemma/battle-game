package io.dylemma.battle

trait Target

case class CombattantTarget(combattant: Combattant) extends Target
case object EnvironmentTarget extends Target
case object NoTarget extends Target
// TODO: case class PositionTarget(position: Position) extends Target

trait TargetHelpers {
	trait TargetProjector[T] {
		def project(target: Target): Option[T]
	}

	implicit class TargetProjectable(target: Target) {
		def project[T: TargetProjector] = implicitly[TargetProjector[T]].project(target)
	}

	implicit object TargetCombattantProjector extends TargetProjector[Combattant] {
		def project(target: Target) = target match {
			case CombattantTarget(c) => Some(c)
			case _ => None
		}
	}

	implicit object TargetHasResourcesProjector extends TargetProjector[HasResources] {
		def project(target: Target) = target match {
			case CombattantTarget(c) => Some(c)
			case _ => None
		}
	}

}