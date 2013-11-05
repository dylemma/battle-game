package io.dylemma.server

import akka.actor.Actor
import akka.actor.DeadLetter
import akka.actor.ActorRef
import akka.actor.Props
import spray.can.Http
import akka.io.IO

class ServerActor(portNum: Int) extends Actor {

	import context.system

	private var webActor: ActorRef = system.deadLetters
	override def preStart = {
		webActor = system.actorOf(Props[BattleWebAPI], "hello-listener")
		IO(Http) ! Http.Bind(webActor, interface = "localhost", port = portNum)
	}

	def receive = {
		case msg => println("Received " + msg)
	}
}