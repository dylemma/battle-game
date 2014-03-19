;(function(Game){

	var GameUI = Game.UI = {}

	GameUI.ActivityIndicator = function ActivityIndicator(gameMaster, parentElement){
		if(this instanceof ActivityIndicator){

			var div = this.div = (function(){
				var d = document.createElement('div')
				var s = d.style
				s.width = '10px'
				s.height = '10px'
				s.display = 'inline-block'
				s.backgroundColor = 'hsl(0, 100%, 50%)'
				return d
			})()

			var hue = 0

			function updateHue(){
				hue += 1
				if(hue > 360) hue -= 360
				var color = 'hsl(' + hue + ', 100%, 50%)'
				div.style.backgroundColor = color
			}


			this.deactivate = gameMaster.addGameLoopListener(updateHue)
			parentElement.appendChild(div)

		} else {
			return new ActivityIndicator(gameMaster, parentElement)
		}
	}

	GameUI.DialogBox = function DialogBox(gameMaster, parentElement){
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
			return new DialogBox(gameMaster, parentElement)
		}
	}

})(this.Game || (this.Game = {}))