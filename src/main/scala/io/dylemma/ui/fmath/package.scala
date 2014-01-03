package io.dylemma.ui

package object fmath {

	val Pi = math.Pi.toFloat

	def rads(angles: Float) = angles * Pi / 180
	def angles(rads: Float) = rads * 180 / Pi

	def cos(rads: Float) = math.cos(rads).toFloat
	def sin(rads: Float) = math.sin(rads).toFloat
	def tan(rads: Float) = math.tan(rads).toFloat
}