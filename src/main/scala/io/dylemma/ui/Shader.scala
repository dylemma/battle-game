package io.dylemma.ui

import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL11

case class Shader(handle: Int) extends AnyVal

class ShaderException(reason: String) extends Exception(reason)

object Shader {
	def loadShader[T: ReadAsString](source: T, shaderType: Int) = {
		val handle = GL20.glCreateShader(shaderType)
		val sourceString = implicitly[ReadAsString[T]].readAsString(source)
		GL20.glShaderSource(handle, sourceString)
		GL20.glCompileShader(handle)
		if (GL20.glGetShader(handle, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			val infoLog = GL20.glGetShaderInfoLog(handle, 512)
			throw new ShaderException(s"Shader compilation failed:\n$infoLog")
		}
		Shader(handle)
	}

	def loadVertexShader[T: ReadAsString](source: T) = {
		val vs = loadShader(source, GL20.GL_VERTEX_SHADER)
		println(s"loaded vertex shader: $vs")
		vs
	}

	def loadFragmentShader[T: ReadAsString](source: T) = {
		val fs = loadShader(source, GL20.GL_FRAGMENT_SHADER)
		println(s"loaded fragment shader: $fs")
		fs
	}
}
