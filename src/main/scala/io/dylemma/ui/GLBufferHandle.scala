package io.dylemma.ui

import org.lwjgl.opengl.GL15

object GLBufferHandle {
	def createNew = GLBufferHandle(GL15.glGenBuffers)
}

case class GLBufferHandle(handle: Int) extends AnyVal {

	def bind[U](target: Int)(body: => U) = {
		GL15.glBindBuffer(target, handle)
		try {
			body
		} finally {
			GL15.glBindBuffer(target, 0)
		}
	}

	def release = {
		GL15.glDeleteBuffers(handle)
	}
}