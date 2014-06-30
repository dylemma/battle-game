Ticker = require('./Game/Ticker')
Keyboard = require('./Game/Input/Keyboard')
Mouse = require('./Game/Input/Mouse')

ticker = new Ticker
keyboard = new Keyboard(ticker)
mouse = new Mouse(ticker)

ticker.on 'tick', ->
	if keyboard._events.length
		console.log JSON.stringify keyboard._events
	if mouse._events.length
		console.log JSON.stringify mouse._events

ticker.start()