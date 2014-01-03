package io.dylemma.ui

import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.Util
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.GLU
import org.lwjgl.BufferUtils

case class ShaderProgram(programId: Int) extends AnyVal {
	def useWith[U](body: => U) = {
		GL20.glUseProgram(programId)
		try {
			body
		} finally {
			GL20.glUseProgram(0)
		}
	}

	def getUniform[T: GLUniformSetter](name: String) = {
		val location = GL20.glGetUniformLocation(programId, name)
		if (location < 0) throw new ShaderException(s"Invalid uniform name: $name")
		GLUniform[T](location)
	}
}

object ShaderProgram {
	def createFrom(vertexShader: Shader, fragmentShader: Shader, attribs: ShaderAttribute*) = {
		val programId = GL20.glCreateProgram
		GL20.glAttachShader(programId, vertexShader.handle)
		GL20.glAttachShader(programId, fragmentShader.handle)

		for (ShaderAttribute(index, name) <- attribs) {
			GL20.glBindAttribLocation(programId, index, name)
		}

		GL20.glLinkProgram(programId)
		if (GL20.glGetProgram(programId, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			val infoLog = GL20.glGetProgramInfoLog(programId, 512)
			throw new ShaderException(s"Failed to link shader program:\n$infoLog")
		}

		GL20.glValidateProgram(programId)
		if (GL20.glGetProgram(programId, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
			val infoLog = GL20.glGetProgramInfoLog(programId, 512)
			throw new ShaderException(s"Failed to validate shader program:\n$infoLog")
		}

		ShaderProgram(programId)
	}

	def load[V: ReadAsString, F: ReadAsString](vertexShaderSource: V, fragmentShaderSource: F, attribs: ShaderAttribute*) = {
		val vertexShader = Shader.loadVertexShader(vertexShaderSource)
		val fragmentShader = Shader.loadFragmentShader(fragmentShaderSource)
		createFrom(vertexShader, fragmentShader, attribs: _*)
	}
}

case class ShaderAttribute(index: Int, name: String)