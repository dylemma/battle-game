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

	hexId = (hex) ->
		{p,q} = hex
		JSON.stringify {p,q}

	selections = []
	addSelection = (d) ->
		selections.unshift d
		if selections.length > 10 then selections.pop()
		ids = {}
		selections.forEach (d) -> ids[hexId d] = 1
		paths.style 'fill', (d) ->
			if ids[hexId d] then 'orange' else 'navy'

		aoeMarkerData = selections.map (d,i) ->
			{tile: d, intensity: (10-i) / 10}

		markers = group.selectAll('.marker').data(aoeMarkerData)
		markers.exit().remove()
		markers.enter().append('svg:path').attr 'class', 'marker'
		markers
			.attr 'd', (m) -> hexGrid.hexPath m.tile.p, m.tile.q, m.intensity * 13
			.style 'fill', 'black'
			.style 'pointer-events', 'none'

	paths = group.selectAll('.hex').data(hexes)

	paths.enter().append('svg:path')
		.attr 'class', 'hex'
		.attr 'd', (d) -> hexGrid.hexPath d.p, d.q, 0 # 19.5
		.style 'fill', 'navy'
		.on 'mouseover', addSelection
	.transition()
	.delay (d,i) ->
		{x,y,dist} = d
		rads = angleOffset x, y
		(dist * 150) + (rads * 300 / tau)
	.duration(1500)
	.ease d3.ease 'elastic'
		.attr 'd', (d) -> hexGrid.hexPath d.p, d.q, 14.5



