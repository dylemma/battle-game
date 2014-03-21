namespaced('Game', 'UI', function(GameUI){

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

})