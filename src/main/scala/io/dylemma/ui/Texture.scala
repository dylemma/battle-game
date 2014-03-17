package io.dylemma.ui

import java.io.File
import java.io.FileInputStream
import de.matthiasmann.twl.utils.PNGDecoder
import PNGDecoder.Format
import java.nio.ByteBuffer
import java.io.InputStream
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.util.Dimension
import org.lwjgl.opengl.GL30

object Texture {

	def loadToBuffer(instr: InputStream): (ByteBuffer, Dimension) = {
		try {
			val decoder = new PNGDecoder(instr)

			val texWidth = decoder.getWidth
			val texHeight = decoder.getHeight

			val buf = ByteBuffer.allocateDirect(4 * texWidth * texHeight)
			decoder.decode(buf, texWidth * 4, Format.RGBA)
			buf.flip

			val dim = new Dimension(texWidth, texHeight)

			buf -> dim
		} finally {
			instr.close
		}
	}

	def loadToGL(instr: InputStream, textureUnit: Int) = {
		val (imgData, imgDim) = loadToBuffer(instr)

		val texId = GL11.glGenTextures
		GL13.glActiveTexture(textureUnit)
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId)

		// All RGB bytes are aligned to each other and each component is 1 byte
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)

		GL11.glTexImage2D(
			GL11.GL_TEXTURE_2D, // target 
			0, // level 
			GL11.GL_RGB, // internalformat 
			imgDim.getWidth, // width 
			imgDim.getHeight, // height 
			0, // border 
			GL11.GL_RGBA, // format 
			GL11.GL_UNSIGNED_BYTE, // type
			imgData //pixels
			)

		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)
	}
}