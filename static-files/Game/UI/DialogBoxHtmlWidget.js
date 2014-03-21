namespaced('Game', 'UI', function(GameUI){

	GameUI.DialogBoxHtmlWidget = function DialogBox(parentElement){
		if(this instanceof DialogBox){

			var outerDiv = document.createElement('div'),
				innerDiv = document.createElement('div'),
				cursorDiv = document.createElement('div'),
				textDiv = document.createElement('div')

			outerDiv.classList.add('dialogBoxOuter')
			innerDiv.classList.add('dialogBoxInner')
			cursorDiv.classList.add('dialogBoxCursor')
			textDiv.classList.add('dialogBoxText')

			innerDiv.appendChild(textDiv)
			outerDiv.appendChild(innerDiv)
			outerDiv.appendChild(cursorDiv)
			parentElement.appendChild(outerDiv)

			var ticker = 0,
				state = 'idle',
				text = ''

			this.updateState = function(stateObj){
				state = stateObj['status']
				ticker = (state == 'waiting') ? ticker + 1 : 0
				text = stateObj['text']

				// set the UI text
				textDiv.innerText = text

				// animate the cursor up and down in a sine wave pattern
				var cursorY = Math.round(Math.sin(ticker * 0.1) * 2 + 2) // [range 0 to 4]
				cursorDiv.style.bottom = cursorY + 'px'

				// show or hide the outer div depending on the state
				outerDiv.style.display = (state == 'idle') ? 'none' : ''
			}

			var instructions = [],
				currentInstruction = undefined

		} else {
			return new DialogBox(parentElement)
		}
	}

})