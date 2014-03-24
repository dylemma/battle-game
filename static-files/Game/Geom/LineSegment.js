namespaced('Game', 'Geom', function(Geom){

	Geom.LineSegment = function LineSegment(){
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

		this.log = function(){ console.log('LineSegment', v) }

		// Update this LineSegment so that its endpoints are specified by
		// origin = {x: originX, y: originY}
		// target = {x: targetX, y: targetY}
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

		// Project the given point onto this vector (using this vector's
		// origin as the axis origin, and direction as the 'x' axis). The
		// projection vector is returned via the 'result' argument, which
		// should be an array; result[0] = the 'x' component of the projection,
		// and result[1] = the 'y' component.
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
	}
})