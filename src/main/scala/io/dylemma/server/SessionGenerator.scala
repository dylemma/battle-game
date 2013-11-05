package io.dylemma.server

import scala.util.DynamicVariable
import java.security.SecureRandom
import akka.util.ByteString
import javax.xml.bind.DatatypeConverter

object SessionGenerator {

	private val rand = new SecureRandom

	def next = {
		val buff = new Array[Byte](16)
		rand.nextBytes(buff)
		DatatypeConverter.printHexBinary(buff)
	}
}