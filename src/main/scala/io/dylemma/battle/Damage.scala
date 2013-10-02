package io.dylemma.battle

sealed trait Damage {
	def amount: Double
}

case class DamageAmount(amount: Double) extends Damage
trait DamageDecorator extends Damage {
	def damage: Damage
	def amount = damage.amount
}

object Damage {
	/** An object that decorates a damage instance by wrapping it with a DamageDecorator.
	  * Instances of `Decorator` may be viewed as an alternate syntax for actual
	  * DamageDecorator constructor calls. Additionally, they may be specified in the
	  * `Damage.apply` method to create a shorthand for many decorations.
	  */
	trait Decorator { def decorate(damage: Damage): DamageDecorator }

	def apply(amount: Double, decorators: Decorator*): Damage = {
		val base = DamageAmount(amount)
		decorators.foldLeft(base: Damage) { (dmg, dec) => dec.decorate(dmg) }
	}

	/** Allows the `x` method on an existing damage instance to add decorations,
	  * e.g. `val d = d0 x DamageType.Blunt`
	  */
	implicit class DamageXDecorator(damage: Damage) {
		def x(decorator: Decorator) = decorator.decorate(damage)
	}

	case class Source(source: Target) extends Decorator {
		def decorate(damage: Damage) = DamageSourceDecorator(source, damage)
	}

	/** Object that adds a CriticalDamage decorator with the specified multiplier*/
	case class Critical(multiplier: Double) extends Decorator {
		def decorate(damage: Damage) = CriticalDamageDecorator(multiplier, damage)
	}

	/** Object that decorates with a plain damage Multiplier */
	case class Multiplier(factor: Double) extends Decorator {
		def decorate(damage: Damage) = DamageMultiplierDecorator(factor, damage)
	}

	/** Object that decorates with a flat damage Bonus */
	case class Bonus(amount: Double) extends Decorator {
		def decorate(damage: Damage) = DamageBonusDecorator(amount, damage)
	}

	/** Treats a DamageType as an object that adds a DamageType decorator */
	implicit class DamageTypeAsDecorator(dtype: DamageType) extends Decorator {
		def decorate(damage: Damage) = DamageTypeDecorator(dtype, damage)
	}

	/** Checks if the `damage` includes a CriticalDamage decorator */
	def isCritical(damage: Damage): Boolean = damage match {
		case CriticalDamageDecorator(_, _) => true
		case dec: DamageDecorator => isCritical(dec.damage)
		case DamageAmount(_) => false
	}

	/** Locates (if possible) the outermost DamageType decorator of the `damage` */
	def damageType(damage: Damage): Option[DamageType] = damage match {
		case DamageTypeDecorator(t, _) => Some(t)
		case dec: DamageDecorator => damageType(dec.damage)
		case DamageAmount(_) => None
	}

	def source(damage: Damage): Option[Target] = damage match {
		case DamageSourceDecorator(s, _) => Some(s)
		case dec: DamageDecorator => source(dec)
		case DamageAmount(_) => None
	}
}

case class DamageTypeDecorator(dtype: DamageType, damage: Damage) extends DamageDecorator
case class DamageSourceDecorator(source: Target, damage: Damage) extends DamageDecorator
case class CriticalDamageDecorator(multiplier: Double, damage: Damage) extends DamageDecorator {
	override def amount = super.amount * multiplier
}
case class DamageMultiplierDecorator(factor: Double, damage: Damage) extends DamageDecorator {
	override def amount = super.amount * factor
}
case class DamageBonusDecorator(bonus: Double, damage: Damage) extends DamageDecorator {
	override def amount = super.amount + bonus
}
