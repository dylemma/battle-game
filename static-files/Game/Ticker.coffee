{EventEmitter} = require 'events'

module.exports = class Ticker extends EventEmitter
	constructor: ->
		@_paused = true
		@_runLoop = =>
			@emit 'pre-tick'
			@emit 'tick'
			@emit 'post-tick'

			requestAnimationFrame @_runLoop if not @_paused
	isPaused: -> @_paused
	pause: -> @_paused = true
	start: ->
		if @_paused
			@_paused = false
			requestAnimationFrame @_runLoop
	addTickListener: (listener) -> @on 'tick', listener
	addPreTickListener: (listener) -> @on 'pre-tick', listener
	addPostTickListener: (listener) -> @on 'post-tick', listener