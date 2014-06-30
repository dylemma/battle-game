{EventEmitter} = require 'events'
Keyboard = require './Input/Keyboard'

module.exports = class DialogBox extends EventEmitter
	constructor: (@_ticker, @_keyboard) ->
		@_messageQueue = []
		@_currentMessage = null
		@_status = 'idle'
		@_text = ''
		@_progress = 0
		@_total = 0

	getState: ->
		status: @_status
		text: @_text
		progress: @_progress
		total: @_total

	sendMessage: (text, onAdvance) ->
		if typeof onAdvance isnt 'function' then onAdvance = ->
		@_messageQueue.push text: text, onAdvance: onAdvance
		@_advanceIfReady()

	sendMessages: (textsArray, onLastAdvance) ->
		for text, i in textsArray
			onAdvance = if (i == textsArray.length-1) then onLastAdvance else undefined
			@sendMessage text, onAdvance

	###
	Set up the next message in the queue as the current message, as long as
	there isn't already a current message. If no new message is available,
	set the status to `idle`. Otherwise, initialize the new message and attach
	it to the game loop.
	###
	_advanceIfReady: ->
		if @_currentMessage then return

		if @_currentMessage = @_messageQueue.shift()
			# prep the message
			@_status = 'typing'
			@_total = @_currentMessage.text.length
			@_progress = 0
			@_text = ''
			@_ticker.on 'tick', @_onTick
		else
			#clear the state
			@_progress = @_total = 0
			@_text = ''
			@_status = 'idle'

		@_broadcastState()

	###
	Advance the state with the current message. If the progress hasn't
	reached the total, increment it and update the text. Otherwise, set
	the status to 'waiting' and check if the Space key was pressed. If
	so, the current message is finished, and should be transitioned out.
	###
	_onTick: =>
		if not @_currentMessage then return

		if @_progress >= @_total
			# message is done. wait for user input
			@_status = 'waiting'
			@_progress = @_total
			@_text = @_currentMessage.text
			@_broadcastState()

			if @_keyboard.wasKeyPressed Keyboard.KEY_SPACE
				@_finishCurrent()
				@_advanceIfReady()
		else
			@_text = @_currentMessage.text.substring 0, ++@_progress
			@_broadcastState()

	###
	Call the completion function for the current message, and disconnect
	the onTick listener.
	###
	_finishCurrent: ->
		msg = @_currentMessage
		if not msg then return
		@_currentMessage = null
		msg.onAdvance()
		@_ticker.removeListener 'tick', @_onTick

	# Emit the current "state" as a 'state' event
	_broadcastState: ->
		@emit 'state', @getState()