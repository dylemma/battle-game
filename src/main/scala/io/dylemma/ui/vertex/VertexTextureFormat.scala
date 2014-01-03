package io.dylemma.ui.vertex

import io.dylemma.ui.SizeOf._
import org.lwjgl.opengl.GL11

/** VertexFormat part that specifies a texture coordinate for each vertex.
  * Texture coordinates are represented with an "S" and "T" axis. This
  * class's companion object provides an implicit upgrade for VertexBuffers
  * that have a Texture part on their vertices, and adds methods for getting
  * and setting the S and T values for each vertex.
  */
class VertexTextureFormat extends VertexFormatLeaf

object VertexTextureFormat {
	implicit object VertexTextureFormatInfo extends VertexFormatWidth[VertexTextureFormat] {
		val byteWidth = byteSizeOf[Float] * 2
	}
	implicit object VertexTextureDataType extends VertexFormatDataType[VertexTextureFormat] {
		def glType = GL11.GL_FLOAT
		def elementCount = 2
	}

	private val sOffset = byteSizeOf[Float] * 0
	private val tOffset = byteSizeOf[Float] * 1

	implicit class VertexBufferTextureOps[F <: VertexFormat](vb: VertexBuffer[F])(
		implicit width: VertexFormatWidth[F], offset: VertexFormatOffset[F, VertexTextureFormat]) {

		private def baseOffset(index: Int) = index * byteWidthOf[F] + offsetIn[F, VertexTextureFormat]
		private val bb = vb.bytes

		def getS(index: Int) = bb.getFloat(baseOffset(index) + sOffset)
		def getT(index: Int) = bb.getFloat(baseOffset(index) + tOffset)

		def setS(index: Int, s: Float): Unit = bb.putFloat(baseOffset(index) + sOffset, s)
		def setT(index: Int, t: Float): Unit = bb.putFloat(baseOffset(index) + tOffset, t)

		def getST(index: Int) = {
			val off = baseOffset(index)
			(bb.getFloat(off + sOffset), bb.getFloat(off + tOffset))
		}

		def setST(index: Int, st: (Float, Float)): Unit = {
			val off = baseOffset(index)
			bb.putFloat(off + sOffset, st._1)
			bb.putFloat(off + tOffset, st._2)
		}

		def setST(index: Int, s: Float, t: Float): Unit = {
			setST(index, (s, t))
		}
	}
}