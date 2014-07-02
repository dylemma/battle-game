HexGrid = require './Game/Geom/HexGrid'
d3 = require 'd3'
$ = require 'jquery'

hexGrid = new HexGrid(15)
hexes = []
spread = 4
tau = 2 * Math.PI

# Gives atan2(y,x), pushed into the positive range
angleOffset = (x,y) ->
	rads = Math.atan2 y, x
	(-rads + tau) % tau

for radius in [0..6]
	for {p,q} in hexGrid.hexRing radius
		{x,y} = hexGrid.hexToCart p, q
		dist = radius
		hexes.push {x,y,p,q,dist}

$ ->
	svg = d3.select('body').append('svg:svg')
		.attr 'height', 400
		.attr 'width', 400

	group = svg.append('svg:g')
		.attr 'transform', 'translate(200,200)'

	paths = group.selectAll('.hex').data(hexes).enter().append('svg:path')
		.attr 'class', 'hex'
		.attr 'd', (d) -> hexGrid.hexPath d.p, d.q, 0 # 19.5
		.style 'fill', 'navy'
		.on 'mouseover', ->
			d3.select(this).style 'fill', 'orange'
		.on 'mouseout', ->
			d3.select(this).style 'fill', 'navy'
	.transition()
	.delay (d,i) ->
		{x,y,dist} = d
		rads = angleOffset x, y
		(dist * 150) + (rads * 300 / tau)
	.duration(1500)
	.ease d3.ease 'elastic'
		.attr 'd', (d) -> hexGrid.hexPath d.p, d.q, 14.5