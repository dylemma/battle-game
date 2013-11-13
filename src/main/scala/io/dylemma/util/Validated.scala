package io.dylemma.util

sealed trait Validated[+T]
case class Accepted[T](value: T) extends Validated[T]
case object Rejected extends Validated[Nothing]