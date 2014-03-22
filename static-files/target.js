var battlefieldData = {
	fieldWidth: 500,
	fieldHeight: 500,
	battlers: [{
		id: 'hero',
		x: 100,
		y: 100,
		color: 'skyblue'
	},{
		id: 'skeleton1',
		x: 400,
		y: 400,
		color: 'red'
	},{
		id: 'skeleton2',
		x: 300,
		y: 400,
		color: 'red'
	},{
		id: 'skelton3',
		x: 100,
		y: 300,
		color: 'red'
	},{
		id: 'skeleton4',
		x: 200,
		y: 200,
		color: 'red'
	},{
		id: 'skeleton5',
		x: 300,
		y: 300,
		color: 'red'
	}]
}

function translation(x,y){
	return 'translate(' + x + ',' + y + ')'
}
function rotation(r,cx,cy){
	return 'rotate(' + r + ',' + cx + ',' + cy + ')'
}

var mv = new MouseVector()

$(document).ready(function(){
	console.log('ready')

	var svg = d3.select('svg')/*.append('svg:svg')
		.attr('height', battlefieldData.fieldHeight)
		.attr('width', battlefieldData.fieldWidth)*/

	svg.selectAll('.battler').data(battlefieldData.battlers).enter().append('svg:circle')
		.attr('class', 'battler')
		.attr('cx', function(d){ return d.x })
		.attr('cy', function(d){ return d.y })
		.attr('r', 5)
		.style('fill', function(d){ return d.color })

	var hero = battlefieldData.battlers[0]

	lineTargetPicker(svg, function(){ return 500 })

	var hits = battlefieldData.battlers.slice(1)
	hits.forEach(function(d){ d.magnitude = Math.random() * 2 })
	drawHits(svg, hits)

	// var projection = [0, 0]

	d3.select(window).on('mousemove', function(){
		var m = d3.mouse(svg[0][0])
		var mx = m[0]
		var my = m[1]
		mv.updateFromEndpoints(hero.x, hero.y, mx, my)

		// var testPoint = battlefieldData.battlers[1]
		// mv.projectPoint(testPoint.x, testPoint.y, projection)
		// console.log(projection)
	})
})

function MouseVector(){
	var v = {
		length: 0,
		lengthSquared: 0,
		dx: 0,
		dy: 0,
		ux: 1,
		uy: 0,
		originX: 0,
		originY: 0,
		targetX: 0,
		targetY: 0,
		angle: 0,
		radians: 0
	}

	var self = this
	Object.keys(v).forEach(function(key){
		self.__defineGetter__(key, function(){ return v[key] })
	})

	this.updateFromEndpoints = function(originX, originY, targetX, targetY){
		v.originX = originX
		v.originY = originY
		v.targetX = targetX
		v.targetY = targetY

		v.dx = targetX - originX
		v.dy = targetY - originY

		v.lengthSquared = v.dx * v.dx + v.dy * v.dy
		v.length = Math.sqrt(v.lengthSquared)

		if(v.length == 0){
			v.ux = 1
			v.uy = 0
		} else {
			v.ux = v.dx / v.length
			v.uy = v.dy / v.length
		}

		v.radians = Math.atan2(v.dy, v.dx)
		v.angle = v.radians * 180 / Math.PI
	}

	this.projectPoint = function(pointX, pointY, result){
		// U = v.origin to point = [udx, udy]
		// V = the mouse vector = [v.dx, v.dy]
		// W = perpendicular to the mouse vector = [wdx, wdy]
		// dUV = dot(U, V)
		// dUW = dot(U, W)
		var udx = pointX - v.originX,
			udy = pointY - v.originY,
			dUV = v.dx * udx + v.dy * udy,
			wdx = -v.dy,
			wdy = v.dx,
			dUW = udx * wdx + udy * wdy

		// result = [projection.x, projection.y]
		result[0] = dUV / v.length
		result[1] = dUW / v.length
	}

	this.log = function(){ console.log('MouseVector', v)}
}

function lineTargetPicker(svg, getMaxLength){
	var boxRadius = 10

	var hitbox = svg.append('svg:rect')
		.attr('class', 'hitbox')
		.style('fill', 'skyblue')
		.style('opacity', 0.5)
		.attr('height', boxRadius * 2)
		.attr('width', boxRadius * 10)

	function detectHits(){
		var candidates = battlefieldData.battlers.slice(1),
			projection = [0, 0],
			hits = []
		candidates.forEach(function(target){
			mv.projectPoint(target.x, target.y, projection)
			var inRadius = Math.abs(projection[1]) <= boxRadius
			var inSegment = projection[0] >= 0 && projection[0] <= mv.length
			if(inRadius && inSegment) {
				hits.push(target)
				target.distance = projection[0]
			}
		})
		hits.sort(function(a,b){ return a.distance - b.distance })
		var magnitude = 2
		hits.forEach(function(hit){
			hit.magnitude = magnitude
			magnitude *= 0.5
		})
		// console.log(hits.map(function(d){ return d.id }))
		return hits
	}

	function update(){

		var transform =
			translation(mv.originX, mv.originY) +
			translation(0, -boxRadius) +
			rotation(mv.angle, 0, boxRadius)

		var boxLength = Math.min(mv.length, getMaxLength())

		hitbox
			.attr('width', Math.min(mv.length, boxLength))
			.attr('transform', transform)

		var hits = detectHits()
		drawHits(svg, hits)

		requestAnimationFrame(update)
	}
	requestAnimationFrame(update)
}

function testLineTargetIntersect(pointX, pointY){}

function drawHits(svg, hits){

	var selection = svg.selectAll('.target-hit').data(hits, function(d){ return d.id })

	selection.exit().remove()

	selection.enter().append('svg:circle')
		.attr('class', 'target-hit')
		.style('fill', 'none')
		.style('stroke', 'black')
		.attr('r', function(d){ return d.magnitude * 10 + 10 })
		.attr('cx', function(d){ return d.x })
		.attr('cy', function(d){ return d.y })
}