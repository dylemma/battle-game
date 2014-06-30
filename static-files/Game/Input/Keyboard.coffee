module.exports = class Keyboard
	constructor: (ticker) ->
		@_keysDown = {}
		@_events = []

		# clear the events list after each tick
		ticker.on 'post-tick', =>
			@_events.length = 0

		# listen for keydown events
		window.addEventListener 'keydown', (e) =>
			code = e.keyCode
			if not @_keysDown[code]
				@_keysDown[code] = true
				@_events.push type: 'keydown', key: code

		# listen for keyup events
		window.addEventListener 'keyup', (e) =>
			code = e.keyCode
			if @_keysDown[code]
				@_keysDown[code] = false
				@_events.push type: 'keyup', key: code

	isKeyDown: (keyCode) -> !!@_keysDown[keyCode]
	wasKeyPressed: (keyCode) ->
		@_events.some (e) ->
			e.type == 'keydown' and e.key == keyCode
	wasKeyReleased: (keyCode) ->
		@_events.some (e) ->
			e.type == 'keyup' and e.key == keyCode

Keyboard.KEY_SPACE = 32