package io.dylemma.ui

import java.io.File
import scala.io.Source

trait ReadAsString[T] {
	def readAsString(item: T): String
}

object ReadAsString {
	implicit object StringReadAsString extends ReadAsString[String] {
		def readAsString(item: String) = item
	}

	implicit object FileReadAsString extends ReadAsString[File] {
		def readAsString(item: File) = {
			val src = Source.fromFile(item)
			try {
				src.mkString
			} finally {
				src.close
			}
		}
	}
}