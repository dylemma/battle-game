var battlefieldData = {
	fieldWidth: 500,
	fieldHeight: 500,
	battlers: [{
		id: 'skeleton',
		x: 400,
		y: 400,
		color: 'red'
	},{
		id: 'hero',
		x: 100,
		y: 100,
		color: 'skyblue'
	},{
		id:  'skeleton2',
		x: 300,
		y: 400,
		color: 'red'
	}]
}

function translation(x,y){
	return 'translate(' + x + ',' + y + ')'
}
function rotation(r,cx,cy){
	return 'rotate(' + r + ',' + cx + ',' + cy + ')'
}

$(document).ready(function(){
	console.log('ready')

	var svg = d3.select('body').append('svg:svg')
		.attr('height', battlefieldData.fieldHeight)
		.attr('width', battlefieldData.fieldWidth)

	svg.selectAll('.battler').data(battlefieldData.battlers).enter().append('svg:circle')
		.attr('class', 'battler')
		.attr('cx', function(d){ return d.x })
		.attr('cy', function(d){ return d.y })
		.attr('r', 5)
		.style('fill', function(d){ return d.color })

	var hero = battlefieldData.battlers[1]

	var targetBox = svg.append('svg:rect')
		.attr('x', hero.x)
		.attr('y', hero.y)
		.attr('height', 20)
		.attr('width', 300)
		.style('fill', 'rgba(0,128,255,0.5)')
})