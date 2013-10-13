package io.dylemma.battle

sealed trait Party

object Party {
	case object BlueParty extends Party
	case object RedParty extends Party
}

case object PartyAxis extends Axis[Party] {
	override def toString = "party"
}