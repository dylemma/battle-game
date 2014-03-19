package io.dylemma.experimental.server

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.io.IO
import spray.can.Http
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.HttpMethods.{ GET }
import spray.http.Uri
import Uri.Path
import akka.actor.ActorRef

object Main {
	def main(args: Array[String]): Unit = {
		val portNum = 8080
		startServer(8080)
	}

	def startServer(portNum: Int): Unit = {
		val actorSystem = ActorSystem(s"BattleServer$portNum")
		val serverActor = actorSystem.actorOf(Props(classOf[ServerActor], portNum))
		println(s"Started Battle Server on port $portNum")
	}
}

class ServerActor(portNum: Int) extends Actor {
	import context.system

	override def preStart = {
		val handlerActor = system.actorOf(Props[RouterActor])
		IO(Http) ! Http.Bind(handlerActor, interface = "localhost", port = portNum)
	}

	def receive = {
		case msg => println(s"ServerActor received $msg")
	}
}

class RouterActor extends Actor {

	val staticHandler = context.actorOf(Props[StaticResourceHandler])

	def receive = {
		case Http.Connected(remote, local) => sender ! Http.Register(self)

		case HttpRequest(GET, Uri(_, _, Path.Slash(Path.Segment("static", path)), _, _), _, _, _) =>
			staticHandler forward path

		case req @ HttpRequest(method, path, headers, _, _) =>
			val msg = s"You requested $path using '$method', and headers $headers"
			val response = HttpResponse(entity = msg)
			sender ! response
	}
}