module.exports = class DialogBoxHtmlWidget
	constructor: (parentSelector) ->
		$outer = $('<div>').addClass 'dialogBoxOuter'
		$inner = $('<div>').addClass 'dialogBoxInner'
		$cursor = $('<div>').addClass 'dialogBoxCursor'
		$text = $('<div>').addClass 'dialogBoxText'

		$inner.append $text
		$outer.append $inner
		$outer.append $cursor
		$outer.appendTo parentSelector

		tick = 0
		status = 'idle'
		text = ''

		@update = (_status, _text) ->
			status = _status
			tick = if status is 'waiting' then tick + 1 else 0
			text = _text

			# apply the text to the UI
			# console.log 'update', _status, _text
			$text.text(text)

			# animate the cursor up and down in a sine wave pattern
			# with a range of [0, 4]
			cursorY = Math.round(Math.sin(tick * 0.1) * 2 + 2)
			$cursor.css 'bottom', (cursorY+'px')
