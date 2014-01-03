package io.dylemma.ui

import org.lwjgl.opengl.GL30

object GLVertexArrayHandle {
	def createNew = GLVertexArrayHandle(GL30.glGenVertexArrays)
}

case class GLVertexArrayHandle(handle: Int) extends AnyVal {

	def bind[U](body: => U) = {
		GL30.glBindVertexArray(handle)
		try {
			body
		} finally {
			GL30.glBindVertexArray(0)
		}
	}

	def release = {
		GL30.glDeleteVertexArrays(handle)
	}

}