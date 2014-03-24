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

var mv = new Game.Geom.LineSegment()

$(document).ready(function(){
	console.log('ready')

	var svg = d3.select('svg')

	svg.selectAll('.battler').data(battlefieldData.battlers).enter().append('svg:circle')
		.attr('class', 'battler')
		.attr('cx', function(d){ return d.x })
		.attr('cy', function(d){ return d.y })
		.attr('r', 5)
		.style('fill', function(d){ return d.color })

	var hero = battlefieldData.battlers[0]

	lineTargetPicker(svg, 400)

	var hits = battlefieldData.battlers.slice(1)
	hits.forEach(function(d){ d.magnitude = Math.random() * 2 })
	drawHits(svg, hits)

	// var projection = [0, 0]

	d3.select(window).on('mousemove', function(){
		var m = d3.mouse(svg[0][0])
		var mx = m[0]
		var my = m[1]
		mv.updateFromEndpoints(hero.x, hero.y, mx, my)
	})
})

function lineTargetPicker(svg, maxLength){
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
			hits = [],
			length = Math.min(mv.length, maxLength)

		candidates.forEach(function(target){
			mv.projectPoint(target.x, target.y, projection)
			var inRadius = Math.abs(projection[1]) <= boxRadius
			var inSegment = projection[0] >= 0 && projection[0] <= length
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

		var boxLength = Math.min(mv.length, maxLength)

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