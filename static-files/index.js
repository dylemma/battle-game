$(document).ready(function(){

	var gm = Game.Master()
	gm.runGameLoop()
	var body = document.querySelector('body')
	var db = Game.DialogBox(gm)
	var dbui = Game.UI.DialogBoxHtmlWidget(body)
	var listener = function(state){ dbui.updateState(state) }
	db.addStateListener(listener)

	db.sendMessages([
		"Press space to advance",
		"Hello! How are you today?",
		".... are you going to answer?",
		"Okay I guess you're giving me the silent treatment",
		"That's fine... I guess... See you later."
	], function(){ console.log('finished all messages') })

})