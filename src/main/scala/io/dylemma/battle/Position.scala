package io.dylemma.battle

trait Axis[T] {
	def apply(value: T) = Position.point(this, value)
}

/** An abstract representation of a point in an arbitrary space
  * defined by a set of axes (axis... plural).
  */
trait Position {
	/** Return the value of this position along the given axis.
	  * If this position doesn't exist on the given axis, return
	  * `None` instead.
	  */
	def getValue[T](axis: Axis[T]): Option[T]

	/** A complete set of the axes on which this position exists.
	  * This value is necessary for calculating equality and a
	  * hash code.
	  */
	protected def axes: Set[Axis[_]]

	/** Combine this position with `that` position, giving priority
	  * to `that` position in case there is some dimensional overlap
	  * between the two.
	  */
	def ~(that: Position): Position = Position.combine(that, this)

	/** Two positions are considered equal if they represent the
	  * same value on all of the same axes.
	  */
	override def equals(that: Any): Boolean = that match {
		case pos: Position =>
			val a = this.axes ++ pos.axes
			a.forall { ax => this.getValue(ax) == pos.getValue(ax) }
		case _ => false
	}

	/** Compute the hash code by XOR-ing all of the axis and value hashes */
	override def hashCode = {
		var hash = 0
		for { ax <- axes; v <- getValue(ax) }
			hash ^= (ax.hashCode ^ v.hashCode)
		hash
	}

	override def toString = {
		val values = for {
			ax <- axes.iterator
			v <- getValue(ax)
		} yield s"$ax: $v"

		values.mkString("Position(", ", ", ")")
	}
}

object Position {

	def empty: Position = NoPosition
	def point[T](axis: Axis[T], value: T): Position = new Point(axis, value)
	def combine(main: Position, fallback: Position): Position = new CompoundPosition(main, fallback)

	/** 0-Dimensional Position */
	protected object NoPosition extends Position {
		protected def axes = Set.empty
		def getValue[T](axis: Axis[T]) = None
	}

	/** 1-Dimensional Position */
	protected class Point[T](axis: Axis[T], value: T) extends Position {
		def getValue[T1](a: Axis[T1]) =
			if (a == axis) Some(value.asInstanceOf[T1]) else None

		protected def axes = Set(axis)
	}

	/** Combination of 2 positions, for N-Dimensional Position */
	protected class CompoundPosition(main: Position, fallback: Position) extends Position {
		def getValue[T](axis: Axis[T]) =
			main.getValue(axis) orElse fallback.getValue(axis)

		protected def axes = main.axes ++ fallback.axes
	}

}

trait HasChangingPosition {
	private var _position = Position.empty

	def position = _position
	def position_=(newPos: Position) = { _position = newPos }
}