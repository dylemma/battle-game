findEvent = (mouse, type, button) ->
	result = undefined
	mouse._events.forEach (e) ->
		if e.type is type and e.button is button then result = e
	result

module.exports = class Mouse
	constructor: (ticker) ->
		@x = 0
		@y = 0
		@_events = []
		@_pressedButtons = {}

		# subscribe to click-ish events
		['mousedown', 'mouseup', 'click'].forEach (type) =>
			window.addEventListener type, (e) =>
				btn = e.which

				# update which buttons are up/down
				@_pressedButtons[btn] = true if type is 'mousedown'
				@_pressedButtons[btn] = false if type is 'mouseup'

				# update the position
				@x = e.x
				@y = e.y

				@_events.push type: type, x: @x, y: @y, button: btn

		# Prevent the right-click context menu from popping up, since it causes
		# problems with other mouse presses (like missing the mouseup event)
		window.addEventListener 'contextmenu', (e) -> e.preventDefault()

		# Clear the events list after each tick
		ticker.on 'post-tick', => @_events.length = 0

	isButtonDown: (button) -> !!@_pressedButtons[buttonId]
	getMouseDown: (button) -> findEvent @, 'mousedown', button
	getMouseUp: (button) -> findEvent @, 'mouseup', button