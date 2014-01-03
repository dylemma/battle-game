package io.dylemma.ui.vertex

import io.dylemma.ui.SizeOf._
import org.lwjgl.opengl.GL11

/** VertexFormat part that specifies a spatial position for each vertex.
  * Coordinates are represented in "XYZW" space (W should be 1, generally).
  * This class's companion object provides an implicit upgrade for
  * VertexBuffers that have a position on their vertices, adding methods
  * to get and set the X, Y, Z, and W values for each vertex.
  */
class VertexPositionFormat extends VertexFormatLeaf

object VertexPositionFormat {
	implicit object VertexPositionFormatWidth extends VertexFormatWidth[VertexPositionFormat] {
		val byteWidth = byteSizeOf[Float] * 4
	}
	implicit object VertexPositionDataType extends VertexFormatDataType[VertexPositionFormat] {
		def glType = GL11.GL_FLOAT
		def elementCount = 4
	}

	private val xOffset = byteSizeOf[Float] * 0
	private val yOffset = byteSizeOf[Float] * 1
	private val zOffset = byteSizeOf[Float] * 2
	private val wOffset = byteSizeOf[Float] * 3

	implicit class VertexBufferPositionOps[F <: VertexFormat](vb: VertexBuffer[F])(
		implicit width: VertexFormatWidth[F], offset: VertexFormatOffset[F, VertexPositionFormat]) {

		private def baseOffset(index: Int) = index * byteWidthOf[F] + offsetIn[F, VertexPositionFormat]
		private val bb = vb.bytes

		def getX(index: Int) = bb.getFloat(baseOffset(index) + xOffset)
		def getY(index: Int) = bb.getFloat(baseOffset(index) + yOffset)
		def getZ(index: Int) = bb.getFloat(baseOffset(index) + zOffset)
		def getW(index: Int) = bb.getFloat(baseOffset(index) + wOffset)

		def setX(index: Int, x: Float): Unit = bb.putFloat(baseOffset(index) + xOffset, x)
		def setY(index: Int, y: Float): Unit = bb.putFloat(baseOffset(index) + yOffset, y)
		def setZ(index: Int, z: Float): Unit = bb.putFloat(baseOffset(index) + zOffset, z)
		def setW(index: Int, w: Float): Unit = bb.putFloat(baseOffset(index) + wOffset, w)

		def getXYZW(index: Int) = {
			val off = baseOffset(index)
			(
				bb.getFloat(off + xOffset),
				bb.getFloat(off + yOffset),
				bb.getFloat(off + zOffset),
				bb.getFloat(off + wOffset))
		}

		def setXYZW(index: Int, xyzw: (Float, Float, Float, Float)): Unit = {
			val off = baseOffset(index)
			bb.putFloat(off + xOffset, xyzw._1)
			bb.putFloat(off + yOffset, xyzw._2)
			bb.putFloat(off + zOffset, xyzw._3)
			bb.putFloat(off + wOffset, xyzw._4)
		}

		def setXYZW(index: Int, x: Float, y: Float, z: Float, w: Float): Unit = {
			setXYZW(index, (x, y, z, w))
		}

		def setXYZ(index: Int, xyz: (Float, Float, Float)): Unit = {
			setXYZW(index, (xyz._1, xyz._2, xyz._3, 1))
		}

		def setXYZ(index: Int, x: Float, y: Float, z: Float): Unit = {
			setXYZW(index, (x, y, z, 1))
		}
	}
}