package io.dylemma.experimental.server

import akka.actor.ActorRef
import akka.actor.Actor

case class ActionID(id: Int) extends AnyVal
case class TargetID(id: Int) extends AnyVal

case class AwaitSimple(actions: Map[String, ActionID])

class UIReceiverActor extends Actor {

	def awaiting(actions: Map[String, ActionID], receiver: ActorRef): Actor.Receive = {
		case action: String if actions contains action =>
			val actionId = actions(action)
			receiver ! actionId
	}

	def receive = {
		case AwaitSimple(actions) =>
			context.become { awaiting(actions, sender) }
	}
}