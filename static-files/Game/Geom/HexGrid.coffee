###
Hex Grid Axes:

P = Horizontal Axis [==]
	+p = { x: 1, y: 0 }

Q = Diagonal Axis [\\]
	Oriented at 150 degrees
	q = { x: cos(150), y: sin(150) }

R = Secondary Diagonal Axis [//]
	Oriented at 30 degrees
	+r = { x: cos(60), y: sin(60) }

###

rad2deg = (rad) -> rad * 180 / Math.PI
deg2rad = (deg) -> deg * Math.PI / 180

###
In this hex system, each hex has vertical edges
on its left and right, and points on its top and bottom.
###
hexCornerAngles = {
	upperRight: Math.PI *  1 / 6
	top:        Math.PI *  3 / 6
	upperLeft:  Math.PI *  5 / 6
	lowerLeft:  Math.PI *  7 / 6
	bottom:     Math.PI *  9 / 6
	lowerRight: Math.PI * 11 / 6
}



###
The distance from the center of a hex to one of its edges, i.e. by travelling
along one of the P/Q/R axes. This value is equal to cos(30 deg), or sqrt(3/4).
###
hexDistToEdge = Math.sqrt(0.75)

###
PAxis is a vector that points from the center of a hex to its right edge.
The vector is aligned with the horizontal X axis.
###
PAxisAngle = 0
PAxis = {
	x: Math.cos(PAxisAngle) * hexDistToEdge
	y: Math.sin(PAxisAngle) * hexDistToEdge
}

###
QAxis is a vector that points from the center of a hex to its upper-left edge.
The vector is oriented at 60 degrees above horizontal right (or 120 degrees
from horizontal right).
###
QAxisAngle = Math.PI * 2 / 3
QAxis = {
	x: Math.cos(QAxisAngle) * hexDistToEdge
	y: Math.sin(QAxisAngle) * hexDistToEdge
}

###
RAxis is a vector that points from the center of a hex to its upper-right edge.
The vector is oriented at 60 degrees above horizontal right.
###
RAxisAngle = Math.PI * 1 / 3
RAxis = {
	x: Math.cos(RAxisAngle) * hexDistToEdge
	y: Math.sin(RAxisAngle) * hexDistToEdge
}

###
Creates a triangular cosine function with the given `height` as the amplitude.
The period of the wave is 4*height, e.g. for a height of 3, and input values
starting at 0, the outputs would be

    [3, 2, 1, 0, -1, -2, -3, -2, -1, 0, 1, 2, 3, 2, 1, ...]
###
triangleCos = (height) ->
	p = height * 4
	a = height * 2
	(x) ->
		# For negative numbers, push them into the positive range,
		# so that the mod division will return a positive number.
		if x < 0 then x = (x % p + p)

		Math.abs(x % p - a) - height

clamp = (min,max) ->
	(x) ->
		if x > max then max
		else if x < min then min
		else x

module.exports = class HexGrid
	constructor: (@radius) ->
		@innerRadius = @radius * hexDistToEdge
	hexToCart: (p,q) -> {
		x: 2 * @radius * (PAxis.x * p + QAxis.x * q)
		y: 2 * @radius * (PAxis.y * p + QAxis.y * q)
	}
	hexPath: (cp,cq, radius=@radius) ->
		d = ''
		M = (x,y) -> d += "M#{x} #{y} "
		L = (x,y) -> d += "L#{x} #{y} "
		center = @hexToCart(cp, cq)
		cx = center.x
		cy = center.y
		cornerPos = (angle) ->
			x = cx + radius * Math.cos angle
			y = cy + radius * Math.sin angle
			{x,y}
		# start at the end
		corner = cornerPos(Math.PI * 11 / 6)
		{x, y} = cornerPos(Math.PI * 11 / 6)
		M x, y

		# loop around
		for s in [1..11] by 2
			{x, y} = cornerPos(Math.PI * s / 6)
			L x, y
		d

	###
	Creates an array of Hex Coordinates that form a ring
	around the origin, such that each coordinate's tile
	distance from the origin is equal to the given `radius`.
	###
	hexRing: (radius) ->
		if radius is 0
			[{ p:0, q:0 }]
		else
			radius = Math.abs(radius)
			height = (radius) * 3 / 2
			y = triangleCos height
			count = radius * 6
			c = clamp(-radius, radius)
			pxOffset = -height + radius
			qxOffset = -height
			for x in [0..count-1]
				p = c(y(x + pxOffset))
				q = c(y(x + qxOffset))
				{p,q}
