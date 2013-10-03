package io.dylemma.battle

case class BattleParty(id: Int)
case class BattlePartyMembership(party: BattleParty, member: Combattant) extends BattleModifier
case class BattlePartyAffiliation(party1: BattleParty, party2: BattleParty, affiliation: Affiliation) extends BattleModifier

trait HasBattleParties {
	val modifiers: Set[BattleModifier]

	lazy val partyMemberships = modifiers.collect { case BattlePartyMembership(p, m) => m -> p }.toMap

	lazy val partyAffiliations = {
		val kv = modifiers.collect {
			case BattlePartyAffiliation(party1, party2, affiliation) if party1 != party2 =>
				val rawPair = party1 -> party2
				val pair = // put the lower-id party first
					if (party2.id < party1.id) party2 -> party1
					else party1 -> party2
				pair -> affiliation
		}
		kv.toMap
	}

	def getAffiliation(left: Combattant, right: Combattant): Option[Affiliation] = {
		for {
			leftParty <- partyMemberships get left
			rightParty <- partyMemberships get right
			affiliation <- getAffiliation(leftParty, rightParty)
		} yield affiliation
	}

	def getAffiliation(left: BattleParty, right: BattleParty): Option[Affiliation] = {
		val key = if (right.id < left.id) right -> left else left -> right
		partyAffiliations get key
	}
}