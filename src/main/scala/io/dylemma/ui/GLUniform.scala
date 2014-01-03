package io.dylemma.ui

import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20

case class GLUniform[T: GLUniformSetter](location: Int) {
	private val setter = implicitly[GLUniformSetter[T]]

	def setValue(value: T) = setter.setUniform(location, value)
}

trait GLUniformSetter[T] {
	def setUniform(uniformLocation: Int, data: T): Unit
}

object GLUniformSetter {
	implicit object Matrix4fUniformSetter extends GLUniformSetter[Matrix4f] {
		val buf = BufferUtils.createFloatBuffer(16)

		def setUniform(uniformLocation: Int, matrix: Matrix4f): Unit = {
			buf.clear
			matrix.store(buf)
			buf.flip
			GL20.glUniformMatrix4(uniformLocation, false, buf)
		}
	}
}