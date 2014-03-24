namespaced('Game', function(Game){

	Game.Dispatcher = function Dispatcher(/*eventTypes...*/){

		if(!(this instanceof Dispatcher)) {
			console.log('did not use new')
			var inst = Object.create(Dispatcher.prototype)
			Dispatcher.apply(inst, arguments)
			return inst
		}

		var listenerId = 0
		function nextListenerId(){ return listenerId++ }

		var listenerMap = {}

		function addListener(type, f){
			var id = nextListenerId()
			var listeners = listenerMap[type] || (listenerMap[type] = [])
			var entry = {'id': id, 'f': f}
			listeners.push(entry)
			return id
		}

		function removeListener(type, id){
			var listeners = listenerMap[type]
			if(!listeners) return

			for(var i=listeners.length - 1; i>=0; i--){
				if(listeners[i].id == id){
					listeners.splice(i, 1)
				}
			}
		}

		function dispatchEvent(type, data){
			var listeners = listenerMap[type]
			if(!listeners) return

			listeners.forEach(function(entry){
				entry.f(data)
			})
		}

		// Create the `addListener` function in the `self`,
		// which allows `f` to be called when an event of the
		// given `type` is dispatched. Returns a function that
		// can be called to remove the added listener.
		this.addListener = function(type, f){
			var id = addListener(type, f)
			return function(){
				removeListener(type, id)
			}
		}

		function dispatcherFunc(type){
			return function(data){
				dispatchEvent(type, data)
			}
		}

		for(var i=0; i<arguments.length; i++){
			var type = arguments[i]
			this[type] = dispatcherFunc(type)
		}
	}

})