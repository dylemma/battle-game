namespaced('Game', function(Game){

	Game.Master = function GameMaster(){
		if(this instanceof GameMaster){
			// init this
			this.paused = false
			this.loopListenerId = 0
			this.loopListeners = []
			this.keyboard = Game.Input.Keyboard().listenOn(window)

			this.pause = GameMaster_pause(this)
			this.unpause = GameMaster_unpause(this)
			this.runGameLoop = GameMaster_runGameLoop(this)
			this.addGameLoopListener = GameMaster_addGameLoopListener(this)
		} else {
			return new GameMaster()
		}
	}

	function GameMaster_pause(self){
		return function(){
			self.paused = true
		}
	}

	function GameMaster_unpause(self){
		return function(){
			if(self.paused){
				self.paused = false
				requestAnimationFrame(self.runGameLoop)
			}
		}
	}

	function GameMaster_runGameLoop(self){
		return function(){
			self.loopListeners.forEach(function(listener){
				listener.onLoop()
			})
			if(!self.paused) requestAnimationFrame(self.runGameLoop)
		}
	}

	function GameMaster_addGameLoopListener(self){
		return function(onLoop){
			var listenerId = self.loopListenerId++
			var listener = {'id': listenerId, 'onLoop': onLoop}
			self.loopListeners.push(listener)

			// return a function that can be used to remove this listener
			return function(){
				self.loopListeners = self.loopListeners.filter(function(L){
					return L.id != listenerId
				})
			}
		}
	}

})