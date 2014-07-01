HexGrid = require './Game/Geom/HexGrid'
d3 = require 'd3'
$ = require 'jquery'

hexGrid = new HexGrid(20)
console.log(hexGrid)
hexes = []
spread = 4
for p in [-spread..spread]
	for q in [-spread..spread]
		if Math.abs(p-q) <= spread
			{x,y} = hexGrid.hexToCart p, q
			hexes.push {x,y,p,q}

$ ->
	svg = d3.select('body').append('svg:svg')
		.attr 'height', 400
		.attr 'width', 400

	group = svg.append('svg:g')
		.attr 'transform', 'translate(200,200)'

	paths = group.selectAll('.hex').data(hexes).enter().append('svg:path')
		.attr 'class', 'hex'
		.attr 'd', (d) -> hexGrid.hexPath d.p, d.q, 19.5
		.style 'fill', 'navy'
		.on 'mouseover', ->
			d3.select(this).style 'fill', 'orange'
		.on 'mouseout', ->
			d3.select(this).style 'fill', 'navy'