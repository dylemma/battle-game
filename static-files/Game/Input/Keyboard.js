namespaced('Game', 'Input', function(Input){

	Input.Keyboard = function Keyboard(looper){
		if(!(this instanceof Keyboard)){
			return new Keyboard(looper)
		}

		var keysDown = {},
			events = [],
			self = this

		this.isKeyDown = function(keyCode){
			return !!keysDown[keyCode]
		}

		this.wasKeyPressed = function(keyCode){
			for(var i=0; i<events.length; i++){
				var e = events[i]
				if(e.type == 'keydown' && e.key == keyCode) return true
			}
			return false
		}

		this.wasKeyReleased = function(keyCode){
			for(var i=0; i<events.length; i++){
				var e = events[i]
				if(e.type == 'keyup' && e.key == keyCode) return true
			}
			return false
		}

		this.__defineGetter__('events', function(){ return events })

		window.addEventListener('keydown', function(e){
			var code = e.keyCode

			if(!keysDown[code]){
				keysDown[code] = true
				events.push({type: 'keydown', key: code})
			}
		})
		window.addEventListener('keyup', function(e){
			var code = e.keyCode

			if(keysDown[code]){
				keysDown[code] = false
				events.push({type: 'keyup', key: code})
			}
		})

		looper.addPostLoopListener(function(){
			events.length = 0
		})
	}

})