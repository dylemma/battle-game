HexGrid = require './Game/Geom/HexGrid'
d3 = require 'd3'
$ = require 'jquery'

hexGrid = new HexGrid(20)
console.log(hexGrid)
hexes = []
spread = 5
for p in [-spread..spread]
	for q in [-spread..spread]
		if Math.abs(p-q) <= spread
			hexes.push hexGrid.hexToCart(p,q)

$ ->
	svg = d3.select('body').append('svg:svg')
		.attr 'height', 400
		.attr 'width', 400

	group = svg.append('svg:g')
		.attr 'transform', 'translate(200,200)'

	dots = group.selectAll('dot').data(hexes).enter().append('svg:circle')
		.attr 'r', hexGrid.innerRadius
		.attr 'cx', (d) -> d.x
		.attr 'cy', (d) -> d.y
		.style 'fill', 'steelblue'