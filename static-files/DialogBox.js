// DialogBox:
//
// This file defines the DialogBox class, which represents and manages a virtual
// dialog box that displays messages that are sent to it, waiting for the use to
// press space after each 'section'. Changes in the state can be monitored by
// listener functions, registered by calling `addStateListener`. The state advances
// over time as part of a GameMaster's game loop.
//
// The state has four important properties:
// `status` - one of [idle|typing|waiting]
// `text` - the currently-typed text. This will be a substring of the current message
//          while the status is `typing`. When idle, the text should be empty. When
//          waiting, the text should be equal to the text that was originally sent.
// `progress` - The number of 'steps' taken as part of the typing process.
// `total` - The total number of 'steps' that must be taken to complete typing.

;(function(Game){

	Game.DialogBox = function DialogBox(gameMaster){
		if(this instanceof DialogBox){
			InitializeDialogBox(this, gameMaster)
		} else {
			return new DialogBox(gameMaster)
		}
	}

	var SpaceKeyCode = 32

	function InitializeDialogBox(self, gameMaster){

		/*******************\
		| Private Variables |
		\*******************/

		var messageQueue = [],
			currentMessage = null,
			stateListeners = [],
			state = {
				status: 'idle',
				text: '',
				progress: 0,
				total: 0
			}

		/******************\
		| Public Functions |
		\******************/

		self.sendMessage = function(text, onAdvance){
			if(typeof onAdvance != 'function') onAdvance = function(){}
			var msg = {'text': text, 'onAdvance': onAdvance}
			messageQueue.push(msg)
			advanceIfReady()
		}

		self.sendMessages = function(textsArray, onFinishedAll){
			var i, len = textsArray.length
			for(i=0; i<len; i++){
				var onAdvance = (i == len-1) ? onFinishedAll : undefined
				self.sendMessage(textsArray[i], onAdvance)
			}
		}

		self.addStateListener = function(listener){
			if(typeof listener == 'function'){
				stateListeners.push(listener)
				listener(state)
			}
		}

		self.removeStateListener = function(listener){
			stateListeners = stateListeners.filter(function(f){
				return f !== listener
			})
		}

		/************************\
		| Private Implementation |
		\************************/

		// Set up the next message in the queue as the current message, if there isn't
		// already a current message. If no new message is available, set the status to
		// 'idle'. Otherwise, initialize the new message and attach it to the game loop.
		function advanceIfReady(){
			if(currentMessage) return

			currentMessage = messageQueue.shift()
			if(currentMessage){
				// prep the message
				state.status = 'typing'
				state.total = currentMessage.text.length
				state.progress = 0
				state.text = ''

				currentMessage.removeLoopLogic = gameMaster.addGameLoopListener(function(){
					runLoopLogic()
				})
			} else {
				// clear the state
				state.progress = state.total = 0
				state.text = ''
				state.status = 'idle'
			}

			broadcastState()
		}

		// Advance the state with the current message. If the progress hasn't
		// reached the total, increment it and update the text. Otherwise, set
		// the status to 'waiting' and check if the Space key was pressed. If
		// so, the current message is finished, and should be transitioned out.
		function runLoopLogic(){
			if(!currentMessage) return

			if(state.progress >= state.total){
				state.status = 'waiting'
				state.progress = state.total
				state.text = currentMessage.text
				broadcastState()

				if(checkForSpacePressed()){
					finishCurrent()
					advanceIfReady()
				}
			} else {
				state.text = currentMessage.text.substring(0, ++state.progress)
				broadcastState()
			}
		}

		// Check the gameMaster's keyboard to see if the Space key was pressed
		function checkForSpacePressed(){
			var events = gameMaster.keyboard.takeEvents()
			for(var i in events){
				var e = events[i]
				if(e.type == 'keydown' && e.keyCode == SpaceKeyCode) return true
			}
			return false
		}

		// call the completion function for the current message, and
		// disconnect it from the game loop
		function finishCurrent(){
			var msg = currentMessage
			if(!msg) return
			currentMessage = null
			msg.removeLoopLogic()
			msg.onAdvance()
		}

		// Send the current `state` to each state listener function
		function broadcastState(){
			stateListeners.forEach(function(listener){
				listener(state)
			})
		}
	}

})(this.Game || (this.Game = {}))