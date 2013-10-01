/*
 * This script draws a grid (using D3) to show the damage modifier calculated from
 * Attack (X axis) and Defense (Y axis). Boxes in the grid correspond to an attack
 * and defense value, and are color coded based on the intensity of the modifier.
 * White is 0, Dark Red is 1, and Black is anything much higher.
 */
$(document).ready(function(){
	
	var vizHeight = 800,
		vizWidth = 800,
		xLength = 100,
		yLength = 100,
		container = document.getElementById('graph'),
		tooltip = d3.select('#tooltip'),
		viz = d3.select(container).append('svg:svg').attr('height', vizHeight).attr('width', vizWidth),
		color = d3.scale.linear().domain([0,1]).range(['white', 'darkred']),
		xScale = d3.scale.linear().domain([0, xLength+1]).range([0, vizWidth]),
		yScale = d3.scale.linear().domain([0, yLength+1]).range([vizHeight, 0]),
		grid = new Array(xLength * yLength),
		i,j
	
	for(i=0; i<xLength; ++i){
		for(j=0; j<yLength; ++j){
			grid[i + j*yLength] = {x:i, y:j}
		}
	}
	
	function damageFormula(attack, defense){
		return (attack * 0.5) / (defense + 1)
	}
	
	viz.selectAll('rect.block')
		.data(grid)
	.enter().append('svg:rect')
		.attr('class', 'block')
		.style('fill', function(d){ return color(damageFormula(d.x, d.y)) })
		.attr('x', function(d){ return xScale(d.x) })
		.attr('y', function(d){ return yScale(d.y) })
		.attr('height', function(d){ return yScale(d.y - 1) - yScale(d.y) -1 })
		.attr('width', function(d){ return xScale(d.x + 1) - xScale(d.x) - 1})
		.on('mouseover', function(d){
			var msg = "Attack: " + d.x + ", Defense: " + d.y + " => Damage Mult: " + damageFormula(d.x, d.y)
			tooltip.text(msg)
		})
});