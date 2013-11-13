package io.dylemma.server

import java.security.SecureRandom
import akka.util.ByteString
import javax.xml.bind.DatatypeConverter
import spray.http.HttpCookie
import spray.http.DateTime

class SessionId private (_bytes: ByteString) {
	private def this(_bytes: Array[Byte]) = this(ByteString(_bytes))

	// validate the length of the input _bytes
	if (_bytes.size != SessionId.requiredByteLength) {
		throw new IllegalArgumentException(
			s"Invalid ID length: required ${SessionId.requiredByteLength} bytes.")
	}

	def rawBytes = _bytes

	override def hashCode = rawBytes.hashCode

	override def equals(any: Any) = {
		if (any.isInstanceOf[SessionId]) {
			val that = any.asInstanceOf[SessionId]
			that.rawBytes == this.rawBytes
		} else {
			false
		}
	}

	def toHex: String = DatatypeConverter.printHexBinary(rawBytes.toArray)

	override def toString = s"SessionId($toHex)"
}

object SessionId {
	private[SessionId] val requiredByteLength = 16

	private val rand = new SecureRandom

	def generateNewId: SessionId = {
		val buf = new Array[Byte](requiredByteLength)
		rand.nextBytes(buf)
		new SessionId(buf)
	}

	def fromHex(hex: String): Option[SessionId] = {
		try {
			val bytes = DatatypeConverter.parseHexBinary(hex)
			val bs = ByteString(bytes)
			val sess = new SessionId(bs)
			Some(sess)
		} catch {
			case e: Exception => None
		}
	}

	object Cookie {
		val name = "session"

		def apply(sess: SessionId): HttpCookie = {
			HttpCookie(name, sess.toHex)
		}

		def unapply(cookie: HttpCookie): Option[SessionId] = {
			if (cookie.name == name) fromHex(cookie.content)
			else None
		}

		val deleted = HttpCookie(name, "", expires = Some(DateTime(0)))
	}
}