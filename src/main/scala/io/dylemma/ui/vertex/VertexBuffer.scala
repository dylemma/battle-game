package io.dylemma.ui.vertex

import io.dylemma.ui.SizeOf._

import java.nio.ByteBuffer
import org.lwjgl.BufferUtils

class VertexBuffer[F <: VertexFormat](val bytes: ByteBuffer)
object VertexBuffer {
	def allocate[F <: VertexFormat: VertexFormatWidth](numVertices: Int): VertexBuffer[F] = {
		val numBytes = byteWidthOf[F] * numVertices
		val bytes = BufferUtils.createByteBuffer(numBytes)
		new VertexBuffer[F](bytes)
	}
}

object Testy {

	def main(args: Array[String]): Unit = {

		val vb = VertexBuffer.allocate[VertexPositionFormat :~: VertexColorFormat :~: VertexTextureFormat](4)
		vb.setXYZW(0, (-0.5f, 0.5f, 0, 1))
		vb.setRGBA(0, (1, 0, 0, 1))
		vb.setST(0, (0, 0))

		vb.setXYZW(1, (-0.5f, -0.5f, 0, 1))
		vb.setRGBA(1, (0, 1, 0, 1))
		vb.setST(1, (0, 1))

		vb.setXYZW(2, (0.5f, -0.5f, 0, 1))
		vb.setRGBA(2, (0, 0, 1, 1))
		vb.setST(2, (1, 1))

		vb.setXYZW(3, (0.5f, 0.5f, 0, 1))
		vb.setRGBA(3, (1, 1, 1, 1))
		vb.setST(3, (1, 0))

		val vba = new Array[Byte](vb.bytes.capacity)
		vb.bytes.get(vba)
		println(vba.toList)

		println("position: " + vb.getXYZW(0))
		println("color: " + vb.getRGBA(0))

		val attribList = getVertexAttributePointers[VertexPositionFormat :~: VertexColorFormat :~: VertexTextureFormat](3)
		attribList foreach println

	}
}