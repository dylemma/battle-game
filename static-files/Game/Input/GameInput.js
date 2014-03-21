namespaced('Game', 'Input', function(GameInput){

	function Keyboard(){
		if(this instanceof Keyboard){
			this.keysDown = {}
			this.events = []
		} else {
			return new Keyboard()
		}
	}

	Keyboard.prototype.isKeyDown = function(keyCode){
		return !!this.keysDown[keyCode]
	}

	Keyboard.prototype.takeEvents = function(){
		var events = this.events
		this.events = []
		return events
	}

	Keyboard.prototype.listenOn = function(elem){
		var self = this
		elem.addEventListener('keydown', function(e){
			self.events.push(e)
			self.keysDown[e.keyCode] = true
		})
		elem.addEventListener('keyup', function(e){
			self.events.push(e)
			self.keysDown[e.keyCode] = false
		})
		return self
	}

	GameInput.Keyboard = Keyboard

})