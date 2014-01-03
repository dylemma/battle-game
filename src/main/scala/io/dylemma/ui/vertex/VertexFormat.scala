package io.dylemma.ui.vertex

import org.lwjgl.opengl.GL20

/** A class that describes the components of a Vertex. Possible VertexFormats include
  * (but are not limited to) Position formats, which describe an "XYZW" coordinate for
  * a vertex, and Color formats, which describe an "RGBA" color value for a vertex.
  *
  * Formats are able to be composed (see `MultiVertexFormats`), so that a Vertex might
  * have multiple parts.
  *
  * A VertexFormat is not responsible for any implementation details. It is essentially
  * a compile-time marker that allows a `VertexBuffer` to implicitly gain methods
  * specific to the format's parts.
  */
sealed trait VertexFormat

/** Base trait for VertexFormats that indicate a single "part" of a Vertex,
  * e.g. position or color.
  */
trait VertexFormatLeaf extends VertexFormat

/** Represents a combination of two other VertexFormats.
  *
  * @tparam A The type of the left subformat
  * @tparam B The type of the right subformat
  */
final class VertexFormatTree[A <: VertexFormat, B <: VertexFormat] extends VertexFormat

/** Typeclass for objects that know the number of bytes per Vertex
  * according to a particular VertexFormat.
  */
trait VertexFormatWidth[F <: VertexFormat] {
	def byteWidth: Int
}

/** Typeclass for objects that know how to find a VertexFormat within
  * another VertexFormat, and determine the offset (in bytes) of the
  * inner format's components relative to the overall Vertex.
  *
  * @tparam Outer The type of the overall format
  * @tparam Inner The type of the format that resides within the outer format
  */
trait VertexFormatOffset[Outer <: VertexFormat, Inner <: VertexFormat] {
	def offsetBytes: Int
}

/** Typeclass for objects that know what OpenGL "type" a given
  * VertexFormatLeaf class should use when binding via
  * `glVertexAttribPointer`.
  */
trait VertexFormatDataType[Format <: VertexFormatLeaf] {
	def glType: Int
	def elementCount: Int
}

case class VertexAttributePointer(index: Int, glType: Int, elementCount: Int, byteOffset: Int)

trait VertexFormatPointerGenerator[Format <: VertexFormat] {
	def generatePointers(indexOffset: Int, byteOffset: Int): List[VertexAttributePointer]
}

/** Provides a singleton version of the `VertexFormatOps` trait,
  * as an alternative to mixing in the trait to client code.
  */
object VertexFormatOps extends VertexFormatOps

/** Provides convenience methods for interacting with VertexFormats,
  * like determining the byte width of a generated format, or finding
  * the position (offset) of a subformat within another format.
  */
trait VertexFormatOps { ops =>
	/** Look up the number of bytes per vertex as specified by the given format.
	  * The actual work is done by a VertexFormatWidth which must be implicitly provided for the format.
	  *
	  * @tparam F The type of the VertexFormat
	  */
	def byteWidthOf[F <: VertexFormat: VertexFormatWidth]: Int = {
		implicitly[VertexFormatWidth[F]].byteWidth
	}

	/** Look up the byte offset of a sub-format within a larger format.
	  *
	  * @tparam Outer the larger format
	  * @tparam Inner the sub-format
	  * @return The number of bytes that the sub-format's bytes are offset, from the start of a vertex.
	  */
	def offsetIn[Outer <: VertexFormat, Inner <: VertexFormat](implicit containInfo: VertexFormatOffset[Outer, Inner]): Int = {
		containInfo.offsetBytes
	}

	def getVertexAttributePointers[F <: VertexFormat](indexOffset: Int = 0)(implicit generator: VertexFormatPointerGenerator[F]): List[VertexAttributePointer] = {
		generator.generatePointers(indexOffset, 0)
	}

	def glTypeOf[F <: VertexFormatLeaf: VertexFormatDataType] = implicitly[VertexFormatDataType[F]].glType

	def elementCountOf[F <: VertexFormatLeaf: VertexFormatDataType] = implicitly[VertexFormatDataType[F]].elementCount

	def setVertexAttribPointers[F <: VertexFormat: VertexFormatWidth: VertexFormatPointerGenerator](indexOffset: Int = 0) = {
		val stride = byteWidthOf[F]
		val pointers = getVertexAttributePointers[F](indexOffset)
		println("pointers: " + pointers)
		for (VertexAttributePointer(index, glType, byteWidth, byteOffset) <- pointers) {
			GL20.glVertexAttribPointer(index, byteWidth, glType, false, stride, byteOffset)
		}
	}

	implicit class VertexBufferCanSetAttributePointers[F <: VertexFormat: VertexFormatWidth: VertexFormatPointerGenerator](vb: VertexBuffer[F]) {
		def setVertexAttribPointers(indexOffset: Int = 0) = ops.setVertexAttribPointers[F](indexOffset)
	}

	/** Type alias for VertexFormatTrees, allowing for constructions like {{{
	  * type MyFormat = PositionFormat :~: ColorFormat :~: TextureFormat
	  * }}}
	  */
	type :~:[A <: VertexFormat, B <: VertexFormat] = VertexFormatTree[A, B]

	implicit def getFormatLeafPointerGen[F <: VertexFormatLeaf: VertexFormatDataType: VertexFormatWidth]: VertexFormatPointerGenerator[F] = {
		new FormatLeafPointerGen[F]
	}

	private class FormatLeafPointerGen[F <: VertexFormatLeaf: VertexFormatDataType: VertexFormatWidth]
		extends VertexFormatPointerGenerator[F] {

		def generatePointers(indexOffset: Int, byteOffset: Int): List[VertexAttributePointer] = {
			val attr = VertexAttributePointer(indexOffset, glTypeOf[F], elementCountOf[F], byteOffset)
			attr :: Nil
		}
	}

	implicit def getFormatTreePointerGen[L <: VertexFormat: VertexFormatWidth: VertexFormatPointerGenerator, R <: VertexFormat: VertexFormatPointerGenerator]: VertexFormatPointerGenerator[VertexFormatTree[L, R]] = {
		new FormatTreePointersGen[L, R]
	}

	private class FormatTreePointersGen[L <: VertexFormat: VertexFormatWidth: VertexFormatPointerGenerator, R <: VertexFormat: VertexFormatPointerGenerator]
		extends VertexFormatPointerGenerator[VertexFormatTree[L, R]] {

		def generatePointers(indexOffset: Int, byteOffset: Int): List[VertexAttributePointer] = {
			val leftList = implicitly[VertexFormatPointerGenerator[L]].generatePointers(indexOffset, byteOffset)
			val rightList = implicitly[VertexFormatPointerGenerator[R]].generatePointers(indexOffset + leftList.size, byteOffset + byteWidthOf[L])
			leftList ++ rightList
		}
	}

	/** Implicitly know the format width of any VertexFormatTree whose components' widths are known
	  */
	implicit def vertexFormatTreeWidth[A <: VertexFormat: VertexFormatWidth, B <: VertexFormat: VertexFormatWidth]: VertexFormatWidth[VertexFormatTree[A, B]] = {
		new VertexFormatTreeWidth[A, B]
	}

	/** The width of a Tree format's vertex is equal to the width of its left
	  * and right components combined.
	  */
	protected class VertexFormatTreeWidth[A <: VertexFormat, B <: VertexFormat](
		implicit aInfo: VertexFormatWidth[A], bInfo: VertexFormatWidth[B])
		extends VertexFormatWidth[VertexFormatTree[A, B]] {

		val byteWidth = aInfo.byteWidth + bInfo.byteWidth
	}

	/** Implicitly know how to find the offset of a format within itself */
	implicit def singleVertexFormatOffset[F <: VertexFormatLeaf]: VertexFormatOffset[F, F] = new SingleVertexFormatOffset[F]

	/** A format is always offset by 0 within itself */
	private class SingleVertexFormatOffset[F <: VertexFormatLeaf]
		extends VertexFormatOffset[F, F] {

		def offsetBytes = 0
	}

	/** Implicitly know how to find the offset of a sub-format when it is the left part of a VertexFormatTree */
	implicit def multiVertexFormatLeftOffset[Inner <: VertexFormat, L <: VertexFormat, R <: VertexFormat](
		implicit leftContains: VertexFormatOffset[L, Inner]): VertexFormatOffset[VertexFormatTree[L, R], Inner] = {

		new MultiVertexFormatLeftOffset[Inner, L, R]
	}

	/** When a sub-format is located at the left of a VertexFormatTree, the offset is that sub-format's offset */
	private class MultiVertexFormatLeftOffset[Inner <: VertexFormat, L <: VertexFormat, R <: VertexFormat](
		implicit leftContains: VertexFormatOffset[L, Inner])
		extends VertexFormatOffset[VertexFormatTree[L, R], Inner] {

		def offsetBytes = leftContains.offsetBytes
	}

	/** Implicitly know how to find the offset of a sub-format when it is the right part of a VertexFormatTree */
	implicit def multiVertexFormatContainsRight[Inner <: VertexFormat, L <: VertexFormat, R <: VertexFormat](
		implicit leftInfo: VertexFormatWidth[L], rightContains: VertexFormatOffset[R, Inner]): VertexFormatOffset[VertexFormatTree[L, R], Inner] = {

		new MultiVertexFormatContainsRight[Inner, L, R]
	}

	/** When a sub-format is the right part of a VertexFormatTree, the offset is equal to the left part's width plus the sub-format's own offset */
	private class MultiVertexFormatContainsRight[Inner <: VertexFormat, L <: VertexFormat, R <: VertexFormat](
		implicit leftInfo: VertexFormatWidth[L], rightContains: VertexFormatOffset[R, Inner])
		extends VertexFormatOffset[VertexFormatTree[L, R], Inner] {

		def offsetBytes = leftInfo.byteWidth + rightContains.offsetBytes
	}
}