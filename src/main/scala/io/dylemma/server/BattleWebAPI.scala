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

class BattleWebAPI extends Actor {

	object HasSession {
		def unapply(cookie: HttpCookie): Option[String] = {
			if (cookie.name == "session") Some(cookie.content)
			else None
		}

		def unapply(headers: List[HttpHeader]): Option[String] = {
			val allCookies = headers collectFirst { case Cookie(c) => c } getOrElse Nil
			allCookies collectFirst { case HasSession(s) => s }
		}
	}

	def receive = {
		/** Acknowledge a new connection
		  */
		case Http.Connected(remote, local) =>
			sender ! Http.Register(self)

		/** Serve `GET /remember` by checking (and if necessary, adding) the "session" header.
		  * Response is plaintext, depending on the "session" header.
		  */
		case HttpRequest(GET, path @ Path("/remember"), headers, _, _) => headers match {
			case HasSession(sess) =>
				println("path = " + path)
				sender ! HttpResponse(entity = s"You are remembered as $sess")
			case _ =>
				val newCookie = HttpCookie("session", SessionGenerator.next)
				sender ! HttpResponse(entity = "You are new", headers = List(`Set-Cookie`(newCookie)))
		}

		/** Reply to `GET /ping` with plaintext "PONG"
		  */
		case HttpRequest(GET, Path("/ping"), _, _, _) =>
			sender ! HttpResponse(entity = "PONG")

		/** All other requests are 404. Print out information about the URI that was requested.
		  */
		case HttpRequest(_, uri, _, _, _) =>
			println(s"404 on request to URI:\n\tscheme=${uri.scheme}\n\tauth=${uri.authority}\n\tpath=${uri.path}\n\tquery=${uri.query}\n\tfragment=${uri.fragment}")
			sender ! HttpResponse(StatusCodes.NotFound)
	}
}