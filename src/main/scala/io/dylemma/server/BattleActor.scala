package io.dylemma.server

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Status
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import io.dylemma.util.Accepted
import io.dylemma.util.AsInt
import io.dylemma.util.Rejected
import io.dylemma.util.Validated
import spray.http.HttpEntity.apply
import spray.http.HttpMethods.GET
import spray.http.HttpRequest
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.http.Uri.Path

object BattleActor {
	sealed trait Message
	sealed trait Prompt {
		def validate(value: Response): Validated[Any]
	}
	sealed trait Response
	case object End

	case class StringMessage(s: String) extends Message
	case class StringResponse(s: String) extends Response
	object StringPrompt extends Prompt {
		def validate(value: Response) = value match {
			case StringResponse(s) => Accepted(s)
			case _ => Rejected
		}
		override def toString = "StringPrompt"
	}

}

object BattleProgram {
	import akka.pattern.ask
	import akka.util.Timeout
	import scala.concurrent.duration._
	import scala.concurrent.ExecutionContext

	def run(battleActor: ActorRef)(implicit exc: ExecutionContext): Future[Unit] = {

		def sendString(s: String) = Future.successful {
			battleActor ! BattleActor.StringMessage(s)
		}
		def askString = {
			implicit val timeout = Timeout(300.seconds)
			ask(battleActor, BattleActor.StringPrompt).mapTo[String]
		}
		def sendEnd = Future.successful {
			battleActor ! BattleActor.End
		}

		for {
			_ <- sendString("Hello there")
			_ <- sendString("what is your name?")
			s1 <- askString
			_ <- sendString(s"Hello, $s1")
			_ <- sendString("Oh another person, what's your name?")
			s2 <- askString
			_ <- sendString(s"Oh, let me introduce the two of you. $s1, meet $s2. $s2, meet $s1.")
			_ <- sendEnd
		} yield ()
	}
}

class BattleActor2 extends Actor {

	import BattleActor._

	case class IncomingResponse(promptId: Int, response: Response)
	case class OutgoingPrompt(promptId: Int, prompt: Prompt)

	case class WebRequest(client: ActorRef, response: Option[IncomingResponse])
	case class WebPrompt(program: ActorRef, prompt: OutgoingPrompt)

	sealed trait PromptState
	case object NoPrompt extends PromptState
	case object PromptSatisfied extends PromptState
	case class PromptWaiting(prompt: WebPrompt) extends PromptState

	private val incomingRequests = ListBuffer[WebRequest]()
	private val waitingRequests = ListBuffer[WebRequest]()
	private val waitingMessages = ListBuffer[Message]()
	private var currentPrompt: PromptState = NoPrompt
	private var gotEnd: Boolean = false

	def getAndClear[T](buf: ListBuffer[T]): List[T] = {
		val list = buf.result
		buf.clear
		list
	}

	override def preStart = {
		super.preStart
		import context.dispatcher
		BattleProgram.run(self) onComplete {
			case result => println(s"BattleProgram finished with result $result")
		}
	}

	def receive = {
		case message: Message =>
			println(s"received message: $message")
			waitingMessages += message

		case prompt: Prompt => currentPrompt match {
			case PromptWaiting(_) =>
				println("received a prompt but there was already one unsatisfied")
				val err = new IllegalStateException("Can't ask a new prompt until the last one is answered")
				sender ! Status.Failure(err)
			case _ =>
				println(s"received prompt: $prompt")
				val promptId = Random.nextInt
				val outgoing = OutgoingPrompt(promptId, prompt)
				val webPrompt = WebPrompt(sender, outgoing)
				currentPrompt = PromptWaiting(webPrompt)
				answerWaiters
		}

		case End =>
			println("received End Event")
			gotEnd = true
			currentPrompt = NoPrompt
			answerWaiters

		case HttpRequest(GET, uri @ Path("/battle"), _, _, _) =>
			println(s"received http request for $uri")
			val query = uri.query
			val inc = for {
				promptIdString <- query get "promptId"
				promptId <- AsInt unapply promptIdString
				reply <- query get "reply"
			} yield IncomingResponse(promptId, StringResponse(reply))
			val webRequest = WebRequest(sender, inc)
			incomingRequests += webRequest
			answerWaiters

		case x =>
			println(s"received something unexpected: $x")
	}

	def answerWaiters = currentPrompt match {

		/** No prompt has been set yet. Any incoming requests should have no Response attached,
		  * and they will be upgraded to "waiting" status.
		  */
		case (NoPrompt | PromptSatisfied) if !gotEnd =>
			println(s"currentPrompt = $currentPrompt")
			for (req @ WebRequest(client, responseOpt) <- getAndClear(incomingRequests)) responseOpt match {
				case None =>
					waitingRequests += req
				case Some(response) =>
					val err = s"Unexpected response: $response"
					client ! HttpResponse(StatusCodes.NotFound, entity = err)
			}

		/** A prompt was waiting for responses. Any waiting request already answered a previous
		  * prompt, so they should get a response including this prompt and any messages that
		  * were sent. Any incoming requests should have a Response that can properly answer
		  * this prompt; if they do, they will be upgraded to "waiting".
		  */
		case PromptWaiting(WebPrompt(asker, OutgoingPrompt(outPromptId, prompt))) =>
			println(s"currentPrompt = $currentPrompt")
			val messages = getAndClear(waitingMessages)
			for (WebRequest(client, _) <- getAndClear(waitingRequests)) {
				val resp = "Messages:\n\t" + messages.mkString("\n\t") +
					s"\nPrompt:\n\t$prompt (id = $outPromptId)"
				client ! HttpResponse(entity = resp)
			}

			var gotValidResponse = false
			for (req @ WebRequest(client, responseOpt) <- getAndClear(incomingRequests)) {
				if (gotValidResponse) {
					val err = s"Conflict: another response was already sent and accepted"
					client ! HttpResponse(StatusCodes.Conflict, entity = err)

				} else responseOpt match {
					case None =>
						val err = s"Expected a response to $prompt"
						client ! HttpResponse(StatusCodes.NotFound, entity = err)
					case Some(IncomingResponse(inPromptId, response)) =>
						if (inPromptId == outPromptId) {
							prompt.validate(response) match {
								case Accepted(value) =>
									gotValidResponse = true
									currentPrompt = PromptSatisfied
									asker ! value
									waitingRequests += req
								case Rejected =>
									val err = s"Invalid response: $response"
									client ! HttpResponse(StatusCodes.NotFound, entity = err)
							}
						} else {
							val err = s"Invalid promptId ($inPromptId), expected $outPromptId"
							client ! HttpResponse(StatusCodes.NotFound, entity = err)
						}
				}
			}

		/** No prompt is ready, and and end message has been sent. Any waiting requests will be
		  * fulfilled with whatever messages have been queued, along with a notification that this
		  * is the last interaction. Reject all incoming requests.
		  */
		case NoPrompt =>
			println(s"currentPrompt = $currentPrompt")
			for (WebRequest(client, _) <- getAndClear(incomingRequests)) {
				val err = "The battle is over, no new requests allowed."
				client ! HttpResponse(StatusCodes.NotFound, err)
			}
			val messages = getAndClear(waitingMessages)
			val endResponse = s"Battle is over. Leftover messages: $messages"
			for (WebRequest(client, _) <- getAndClear(waitingRequests)) {
				client ! HttpResponse(entity = endResponse)
			}

	}

}