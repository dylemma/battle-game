package io.dylemma.battle

trait Target

case class CombattantTarget(combattant: Combattant) extends Target

case object EnvironmentTarget extends Target

// TODO: case class PositionTarget(position: Position) extends Target