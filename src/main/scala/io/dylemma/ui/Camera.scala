package io.dylemma.ui

import org.lwjgl.util.vector.{ Vector3f => Vec, Matrix4f => Mat, Vector4f => Vec4 }
import org.lwjgl.util.vector.Matrix3f

class Camera(pUp: Vec) {
	private val up = new Vec(pUp)
	private val pos = new Vec(0, -1, 0)
	private val focus = new Vec(0, 0, 0)

	up.normalise()

	def getLookAtMatrix(result: Mat): Unit = {
		MatrixUtil.getCameraMatrix(result, pos, focus, up)
	}

	def getPosition(as: Vec): Unit = as.set(pos)
	def setPosition(to: Vec) = {
		pos.set(to)
		this
	}
	def movePosition(change: Vec) = {
		Vec.add(pos, change, pos)
		this
	}

	def getFocus(as: Vec): Unit = as.set(focus)
	def setFocus(to: Vec) = {
		focus.set(to)
		this
	}
	def moveFocus(change: Vec) = {
		Vec.add(focus, change, focus)
		this
	}

	/** Change the focus to a point in the given
	  * direction from the camera's current position.
	  * @param direction The direction to face the camera
	  */
	def setFacing(direction: Vec) = {
		Vec.add(pos, direction, focus)
		this
	}

	/** Pan the camera (and its focus) by a given change vector.
	  * @param change The direction to pan the camera
	  */
	def panBy(change: Vec) = {
		Vec.add(pos, change, pos)
		Vec.add(focus, change, focus)
		this
	}

	/** Similar to `panBy`, but instead of specifying a change
	  * vector, this method specifies the camera's new position.
	  * @param newPos The new position for the camera
	  */
	def panTo(newPos: Vec) = {
		val change = new Vec
		Vec.sub(newPos, pos, change)
		pos.set(newPos)
		Vec.add(focus, change, focus)
		this
	}

	/** Keeping a fixed focus point, rotate the camera around it,
	  * based on the `angleRight` and `angleUp`. This method will
	  * keep the camera position the same distance away from the
	  * focus point. Behaves like a third-person viewer.
	  * @param angleRight The angle to rotate around the "up" axis
	  * @param angleUp The angle to rotate towards the "up" axis
	  */
	def thirdPersonRotate(angleRight: Float, angleUp: Float) = {

		// lb = Vec3 pointing from the focus back to the camera
		val lb = new Vec
		Vec.sub(pos, focus, lb)

		// get the horizontal axis from the cross product of up and lb
		val right = new Vec
		Vec.cross(lb, up, right)
		right.normalise()

		// rotate lb according to the two angles and axes,
		// putting the result in lb4
		val m = new Mat()
		Mat.rotate(angleUp, right, m, m)
		Mat.rotate(angleRight, up, m, m)
		val lb4 = new Vec4(lb.x, lb.y, lb.z, 1)
		Mat.transform(m, lb4, lb4)

		// Make sure the new lb vector isn't too close to the up vector.
		// Do this by checking if the cross product of normalized(lb) and
		// up vectors is below a small number (picked 0.02 based on testing).
		lb.set(lb4).normalise()
		Vec.cross(lb, up, right)
		val rightLength = right.length

		// Only apply the change if it wouldn't put us too close to vertical
		if (rightLength > 0.02) {
			lb.set(lb4)
			Vec.add(focus, lb, pos)
		}

		this
	}

	/** Keeping a fixed camera position, rotate the focus around it,
	  * based on the `angleRight` and `angleUp`. Behaves like a first-
	  * person view looking around.
	  * @param angleRight The angle to rotate to the right (around the
	  * "up" axis)
	  * @param angleUp The angle to rotate up (toward the "up" axis)
	  */
	def firstPersonRotate(angleRight: Float, angleUp: Float) = {
		// look = Vec3 pointing from the camera to the focus
		val look = new Vec
		Vec.sub(focus, pos, look)

		// get the "X" axis for rotating up and down
		val right = new Vec
		Vec.cross(look, up, right)
		right.normalise()

		// transform the look vector as look4
		val m = new Mat()
		Mat.rotate(angleUp, right, m, m)
		Mat.rotate(-angleRight, up, m, m)
		val look4 = new Vec4(look.x, look.y, look.z, 1)
		Mat.transform(m, look4, look4)

		// check how close look4 is to the "up" axis
		look.set(look4).normalise()
		Vec.cross(look, up, right)
		val rightLength = right.length

		// do nothing unless if we are too close
		if (rightLength > 0.02) {
			look.set(look4)
			Vec.add(pos, look, focus)
		}

		this
	}

}