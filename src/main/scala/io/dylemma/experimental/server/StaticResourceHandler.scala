package io.dylemma.experimental.server

import akka.actor.Actor
import spray.http.HttpRequest
import spray.http.HttpMethods.GET
import spray.http.Uri
import spray.http.HttpResponse
import java.io.File
import spray.http.Uri.Path
import java.io.BufferedInputStream
import java.io.FileInputStream
import spray.http.MediaTypes
import spray.http.HttpData
import spray.http.HttpEntity

class StaticResourceHandler extends Actor {

	//	private val cache = collection.mutable.Map.empty[String, Option[HttpEntity]]
	private val root = new File("./static-files")

	def getFile(path: Path) = {
		val f = new File(root, path.toString).getCanonicalFile
		val fPath = f.getCanonicalPath
		//		cache.getOrElseUpdate(fPath, {
		if (f.exists && f.canRead) {
			val data = HttpData(f)
			val entity = getMediaType(f) match {
				case None => HttpEntity(data)
				case Some(mt) => HttpEntity(mt, data)
			}
			Some(entity)
		} else {
			None
		}
		//		})
	}

	def getMediaType(file: File) = {
		val name = file.getName
		val idx = name.lastIndexOf('.')
		if (idx >= 0) {
			MediaTypes.forExtension(name.substring(idx + 1))
		} else {
			None
		}
	}

	def receive = {
		case path: Path => getFile(path) match {
			case None => sender ! HttpResponse(404, s"$path not found")
			case Some(data) => sender ! HttpResponse(200, data)
		}
	}
}