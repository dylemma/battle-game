package io.dylemma.battle

sealed trait Affiliation

object Affiliation {
	case object Friendly extends Affiliation
	case object Neutral extends Affiliation
	case object Hostile extends Affiliation
}