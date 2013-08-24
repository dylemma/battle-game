package io.dylemma.battle

/**
 * Damage types are divided into three "realms"; elemental, physical, and spiritual.
 * Each realm is divided into more specific damage types.
 */
sealed trait DamageType {
	def isElemental: Boolean
	def isPhysical: Boolean
	def isSpiritual: Boolean
	def name: String
}

object DamageType {
	
	/*
	 * ELEMENTAL DAMAGE
	 */
	
	sealed abstract class Elemental(val name: String) extends DamageType {
		def isElemental = true
		def isPhysical = false
		def isSpiritual = false
	}
	object Elemental {
		def unapply(dt: DamageType): Option[String] = 
			if(dt.isElemental) Some(dt.name)
			else None
	}
	
	case object Fire extends Elemental("fire")
	case object Water extends Elemental("water")
	case object Wind extends Elemental("wind")
	case object Earth extends Elemental("earth")
	
	/*
	 * PHYSICAL DAMAGE 
	 */
	
	sealed abstract class Physical(val name: String) extends DamageType {
		def isElemental = false
		def isPhysical = true
		def isSpiritual = false
	}
	object Physical {
		def unapply(dt: DamageType): Option[String] = 
			if(dt.isPhysical) Some(dt.name)
			else None
	}
	
	case object Piercing extends Physical("piercing")
	case object Slashing extends Physical("slashing")
	case object Blunt extends Physical("blunt")
	
	/*
	 * SPIRITUAL DAMAGE 
	 */
	
	sealed abstract class Spiritual(val name: String) extends DamageType {
		def isElemental = false
		def isPhysical = false
		def isSpiritual = true
	}
	object Spiritual {
		def unapply(dt: DamageType): Option[String] = 
			if(dt.isSpiritual) Some(dt.name)
			else None
	}
	
	case object Shadow extends Spiritual("shadow")
	case object Poison extends Spiritual("poison")
	case object Mind extends Spiritual("mind")
}