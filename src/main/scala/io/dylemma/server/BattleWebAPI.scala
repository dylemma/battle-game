package io.dylemma.server

import akka.actor.Actor
import spray.can.Http
import spray.http.HttpCookie
import spray.http.HttpHeader
import spray.http.HttpHeaders.Cookie
import spray.http.HttpHeaders.`Set-Cookie`
import spray.http.HttpMethods.GET
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.http.Uri.Path
import spray.http.DateTime
import HeaderExtractors._
import scala.concurrent.ExecutionContext
import scala.util.Success
import scala.util.Failure
import akka.actor.ActorRef
import akka.actor.Props
import spray.http.HttpHeaders

class BattleWebAPI extends Actor {

	private object BattleActors extends SessionVar[ActorRef]

	def receive = {
		/** Acknowledge a new connection */
		case Http.Connected(remote, local) => sender ! Http.Register(self)

		case req @ HttpRequest(_, Path("/battle"), HasSessionCookie(session), _, _) =>
			val handler = BattleActors.getOrElseUpdate(session, {
				println(s"Creating a new BattleActor for $session")
				context.system.actorOf(Props[BattleActor2])
			})
			println(handler)
			handler forward req

		case HttpRequest(_, Path("/battle"), headers, _, _) =>
			sender ! HttpResponse(StatusCodes.BadRequest, entity = s"Expected a valid '${SessionId.Cookie.name}' cookie")

		/** Serve `GET /remember` by checking (and if necessary, adding) the "session" header.
		  * Response is plaintext, depending on the "session" header.
		  */
		case HttpRequest(GET, path @ Path("/remember"), headers, _, _) => headers match {
			case HasSessionCookie(sess) =>
				sender ! HttpResponse(entity = s"You are remembered as $sess")
			case _ =>
				val newCookie = SessionId.Cookie(SessionId.generateNewId)
				sender ! HttpResponse(entity = "You are new", headers = List(`Set-Cookie`(newCookie)))
		}

		case HttpRequest(GET, Path("/forget"), _, _, _) =>
			val newCookie = SessionId.Cookie.deleted
			sender ! HttpResponse(entity = "Who are you again?", headers = List(`Set-Cookie`(newCookie)))

		/** Reply to `GET /ping` with plaintext "PONG"
		  */
		case HttpRequest(GET, Path("/ping"), _, _, _) =>
			sender ! HttpResponse(entity = "PONG")

		case HttpRequest(GET, Path("/pong"), _, _, _) =>
			sender ! HttpResponse(StatusCodes.MultipleChoices)

		/** All other requests are 404. Print out information about the URI that was requested.
		  */
		case HttpRequest(_, uri, _, _, _) =>
			println(s"404 on request to URI:\n\tscheme=${uri.scheme}\n\tauth=${uri.authority}\n\tpath=${uri.path}\n\tquery=${uri.query}\n\tfragment=${uri.fragment}")
			sender ! HttpResponse(StatusCodes.NotFound)
	}
}