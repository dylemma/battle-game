package io.dylemma.ui

import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.ReadableVector3f
import org.lwjgl.BufferUtils
import org.lwjgl.util.vector.Vector3f

object MatrixUtil {

	/** Assign values to the `target` matrix to create a Projection Matrix.
	  *
	  * @param target The matrix whose values will be modified
	  * @param fieldOfView The field of view angle, in radians
	  * @param aspectRatio The ratio of `width / height` for the frustrum
	  * @param nearPlane The distance from the camera to the "near" cutoff
	  * @param farPlane The distance from the camera to the "far" cutoff
	  */
	def getProjectionMatrix(target: Matrix4f, fieldOfView: Float, aspectRatio: Float, nearPlane: Float, farPlane: Float) = {
		val yScale = 1f / math.tan(fieldOfView / 2f)
		val xScale = yScale / aspectRatio
		val frustumLength = farPlane - nearPlane

		// Top Row (target.mx0)
		target.m00 = xScale.toFloat
		target.m10 = 0
		target.m20 = 0
		target.m30 = 0

		// Second Row (target.mx1)
		target.m01 = 0
		target.m11 = yScale.toFloat
		target.m21 = 0
		target.m31 = 0

		// Third Row (target.mx2)
		target.m02 = 0
		target.m12 = 0
		target.m22 = -((farPlane + nearPlane) / frustumLength)
		target.m32 = -2 * nearPlane * farPlane / frustumLength

		// Fourth Row (target.mx3)
		target.m03 = 0
		target.m13 = 0
		target.m23 = -1
		target.m33 = 0
	}

	/** Generates a new matrix that represents a Projection (frustum).
	  *
	  * @param fieldOfView The field of view angle, in radians
	  * @param aspectRatio The ratio of `width / height` for the frustrum
	  * @param nearPlane The distance from the camera to the "near" cutoff
	  * @param farPlane The distance from the camera to the "far" cutoff
	  * @return the new projection matrix
	  */
	def createProjectionMatrix(fieldOfView: Float, aspectRatio: Float, nearPlane: Float, farPlane: Float) = {
		val matrix = new Matrix4f()
		getProjectionMatrix(matrix, fieldOfView, aspectRatio, nearPlane, farPlane)
		matrix
	}

	val scratchBuffer = BufferUtils.createFloatBuffer(16)
	val scratchBuffer2 = BufferUtils.createFloatBuffer(16)
	val scratchVector = new Vector3f()

	def getLookAtMatrix(target: Matrix4f, position: Vector3f, right: Vector3f, up: Vector3f, forward: Vector3f) = {
		scratchBuffer.clear()

		right.store(scratchBuffer)
		scratchBuffer.put(0f)

		up.store(scratchBuffer)
		scratchBuffer.put(0f)

		forward.store(scratchBuffer)
		scratchBuffer.put(0f)

		scratchBuffer.put(-position.x)
		scratchBuffer.put(-position.y)
		scratchBuffer.put(-position.z)
		scratchBuffer.put(1f)
		scratchBuffer.flip()

		target.load(scratchBuffer)
	}

	def getCameraMatrix(m: Matrix4f, eye: Vector3f, center: Vector3f, up: Vector3f) = {
		val f = new Vector3f
		val u = new Vector3f
		val s = new Vector3f

		// f = normalize(center - eye)
		Vector3f.sub(center, eye, f)
		f.normalise()

		// u = normalize(up)
		up.normalise(u)

		// s = normalize(cross(f, u))
		Vector3f.cross(f, u, s)
		s.normalise()

		// u = cross(s,f)
		Vector3f.cross(s, f, u)

		m.setIdentity()
		m.m00 = s.x
		m.m10 = s.y
		m.m20 = s.z
		m.m01 = u.x
		m.m11 = u.y
		m.m21 = u.z
		m.m02 = -f.x
		m.m12 = -f.y
		m.m22 = -f.z

		// get -eye in f, for translating the matrix
		eye.negate(f)
		Matrix4f.translate(f, m, m)
	}

	def roundMatrix(target: Matrix4f) = {
		scratchBuffer.clear()
		scratchBuffer2.clear()
		target.store(scratchBuffer)
		scratchBuffer.flip
		for (i <- 0 until 16) {
			val f = math.round(scratchBuffer.get())
			scratchBuffer2.put(f)
		}
		scratchBuffer2.flip
		target.load(scratchBuffer2)
	}
}