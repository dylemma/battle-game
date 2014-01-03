package io.dylemma.ui

object SizeOf {
	class ByteSizeOf[T](val size: Int)

	def byteSizeOf[T: ByteSizeOf] = implicitly[ByteSizeOf[T]].size

	implicit object ByteSizeOfFloat extends ByteSizeOf[Float](4)
	implicit object ByteSizeOfInt extends ByteSizeOf[Int](4)
}