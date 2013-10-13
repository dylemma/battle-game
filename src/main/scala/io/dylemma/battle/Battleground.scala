package io.dylemma.battle

import io.dylemma.util.BidiMap

case class Battleground(targetPositions: BidiMap[Target, Position], modifiers: BattleModifiers) {

	def positionOf(target: Target): Option[Position] = targetPositions.fromLeft(target)
	def targetAt(pos: Position): Option[Target] = targetPositions.fromRight(pos)

	def positionOf(cmb: Combattant): Option[Position] = positionOf(CombattantTarget(cmb))
	def combattantAt(pos: Position): Option[Combattant] = targetAt(pos).collect {
		case CombattantTarget(cmb) => cmb
	}
}