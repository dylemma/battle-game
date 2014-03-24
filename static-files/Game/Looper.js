namespaced('Game', function(Game){

	// An object that continuously runs a 'loop' function in a browser
	// animation frame. The looper can be paused and unpaused, and can
	// register callback functions to be called before, during, and after
	// each loop iteration.
	Game.Looper = function Looper(){
		var paused = true,
			dispatcher = new Game.Dispatcher('preLoop', 'loop', 'postLoop')

		this.__defineGetter__('paused', function(){ return paused })

		function runLoop(){
			dispatcher.preLoop()
			dispatcher.loop()
			dispatcher.postLoop()

			if(!paused) requestAnimationFrame(runLoop)
		}

		// Pause the looper
		this.pause = function(){
			paused = true
		}

		// Start (unpause) the looper
		this.start = function(){
			if(paused){
				paused = false
				requestAnimationFrame(runLoop)
			}
		}

		// `f` will be called before each loop iteration;
		// this returns a function that can be called to disconnect `f`
		this.addPreLoopListener = function(f){
			return dispatcher.addListener('preLoop', f)
		}

		// `f` will be called for each loop iteration;
		// this returns a function that can be called to disconnect `f`
		this.addLoopListener = function(f){
			return dispatcher.addListener('loop', f)
		}

		// `f` will be called after each loop iteration;
		// this returns a function that can be called to disconnect `f`
		this.addPostLoopListener = function(f){
			return dispatcher.addListener('postLoop', f)
		}

	}

})