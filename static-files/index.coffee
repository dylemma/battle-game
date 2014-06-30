$ = require 'jquery'
Ticker = require './Game/Ticker'
Keyboard = require './Game/Input/Keyboard'
Mouse = require './Game/Input/Mouse'
DialogBox = require './Game/DialogBox'
DialogBoxHtmlWidget = require './Game/UI/DialogBoxHtmlWidget'

ticker = new Ticker
keyboard = new Keyboard(ticker)
mouse = new Mouse(ticker)
dialogBox = new DialogBox(ticker, keyboard)

$(document).ready ->
	ticker.start()

	dialogBoxUI = new DialogBoxHtmlWidget('body')

	dialogBox.on 'state', (s) ->
		dialogBoxUI.update s.status, s.text

	dialogBox.sendMessages [
		"Press space to advance",
		"Hello! How are you today?",
		".... are you going to answer?",
		"Okay I guess you're giving me the silent treatment",
		"That's fine... I guess... See you later."
	], -> console.log 'finished all messages'

	ticker.on 'tick', ->
		mouse._events.forEach (e) ->
			console.log 'mouse event:', JSON.stringify(e)