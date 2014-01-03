package io.dylemma.ui.vertex

import io.dylemma.ui.SizeOf._
import org.lwjgl.opengl.GL11

/** VertexFormat that specifies a color for each vertex. Colors are
  * represented as "RGBA" values. This class's companion object
  * provides an implicit upgrade for VertexBuffers that include a
  * color on their vertices, adding methods to get and set the R, G,
  * B, and A values for each vertex.
  */
class VertexColorFormat extends VertexFormatLeaf

object VertexColorFormat {
	implicit object VertexColorFormatWidth extends VertexFormatWidth[VertexColorFormat] {
		val byteWidth = byteSizeOf[Float] * 4
	}
	implicit object VertexColorDataType extends VertexFormatDataType[VertexColorFormat] {
		def glType = GL11.GL_FLOAT
		def elementCount = 4
	}

	private val rOffset = byteSizeOf[Float] * 0
	private val gOffset = byteSizeOf[Float] * 1
	private val bOffset = byteSizeOf[Float] * 2
	private val aOffset = byteSizeOf[Float] * 3

	implicit class VertexBufferColorOps[F <: VertexFormat](vb: VertexBuffer[F])(
		implicit width: VertexFormatWidth[F], offset: VertexFormatOffset[F, VertexColorFormat]) {

		private def baseOffset(index: Int) = index * byteWidthOf[F] + offsetIn[F, VertexColorFormat]
		private val bb = vb.bytes

		def getR(index: Int) = bb.getFloat(baseOffset(index) + rOffset)
		def getG(index: Int) = bb.getFloat(baseOffset(index) + gOffset)
		def getB(index: Int) = bb.getFloat(baseOffset(index) + bOffset)
		def getA(index: Int) = bb.getFloat(baseOffset(index) + aOffset)

		def setR(index: Int, r: Float): Unit = bb.putFloat(baseOffset(index) + rOffset, r)
		def setG(index: Int, g: Float): Unit = bb.putFloat(baseOffset(index) + gOffset, g)
		def setB(index: Int, b: Float): Unit = bb.putFloat(baseOffset(index) + bOffset, b)
		def setA(index: Int, a: Float): Unit = bb.putFloat(baseOffset(index) + aOffset, a)

		def getRGBA(index: Int) = {
			val off = baseOffset(index)
			(
				bb.getFloat(off + rOffset),
				bb.getFloat(off + gOffset),
				bb.getFloat(off + bOffset),
				bb.getFloat(off + aOffset))
		}

		def setRGBA(index: Int, rgba: (Float, Float, Float, Float)): Unit = {
			val off = baseOffset(index)
			bb.putFloat(off + rOffset, rgba._1)
			bb.putFloat(off + gOffset, rgba._2)
			bb.putFloat(off + bOffset, rgba._3)
			bb.putFloat(off + aOffset, rgba._4)
		}

		def setRGBA(index: Int, r: Float, g: Float, b: Float, a: Float): Unit = {
			setRGBA(index, (r, g, b, a))
		}
	}
}
