package io.dylemma.server

import akka.actor.ActorSystem
import akka.actor.Props

object BattleServer {
	def start(port: Int): BattleServer = new BattleServer(port)

	def main(args: Array[String]): Unit = {
		start(8080)
	}
}

class BattleServer(port: Int) {
	val actorSystem = ActorSystem(s"battle-server-$port")
	private val serverActor = actorSystem.actorOf(Props(classOf[ServerActor], port))
	println(s"Started Battle Server on port $port")
}