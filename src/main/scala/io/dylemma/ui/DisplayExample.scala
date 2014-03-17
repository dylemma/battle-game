package io.dylemma.ui

import org.lwjgl.LWJGLException
import org.lwjgl.opengl.{ Display, DisplayMode }
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL11
import io.dylemma.ui.vertex._
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Matrix
import org.lwjgl.util.vector.{ Vector3f => Vec }
import org.lwjgl.input.Keyboard

object DisplayExample extends HasGLDisplay with MyShaders {

	val modelPos = new Vec(0, 0, 0)
	val modelAngle = new Vec(0, 0, 0)
	val modelScale = new Vec(1, 1, 1)
	val camera = new Camera(new Vec(0, 0, 1))
		.setFocus(new Vec(0, 0, -2))
		.setPosition(new Vec(0, 5, 0))

	def main(args: Array[String]): Unit = {

		setupGLDisplay
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		val model = setupModel
		val shaderProgram = setupShaderProgram

		val uniformProjectionMatrix = shaderProgram.getUniform[Matrix4f]("projectionMatrix")
		val uniformViewMatrix = shaderProgram.getUniform[Matrix4f]("viewMatrix")
		val uniformModelMatrix = shaderProgram.getUniform[Matrix4f]("modelMatrix")

		val projectionMatrix = MatrixUtil.createProjectionMatrix(math.Pi.toFloat / 3, displayWidth.toFloat / displayHeight.toFloat, 0.1f, 100f)

		while (!Display.isCloseRequested) {
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)

			val viewMatrix = new Matrix4f
			val modelMatrix = new Matrix4f

			val panSpeed = fmath.Pi / 180

			val rotateCam = if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
				camera.firstPersonRotate _
			} else {
				camera.thirdPersonRotate _
			}
			if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) rotateCam(0, -panSpeed)
			if (Keyboard.isKeyDown(Keyboard.KEY_UP)) rotateCam(0, panSpeed)
			if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) rotateCam(-panSpeed, 0)
			if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) rotateCam(panSpeed, 0)

			camera.getLookAtMatrix(viewMatrix)

			Matrix4f.scale(modelScale, modelMatrix, modelMatrix)
			Matrix4f.translate(modelPos, modelMatrix, modelMatrix)
			Matrix4f.rotate(modelAngle.z, new Vec(0, 0, 1), modelMatrix, modelMatrix)
			Matrix4f.rotate(modelAngle.y, new Vec(0, 1, 0), modelMatrix, modelMatrix)
			Matrix4f.rotate(modelAngle.x, new Vec(1, 0, 0), modelMatrix, modelMatrix)

			shaderProgram.useWith {

				uniformProjectionMatrix.setValue(projectionMatrix)
				uniformViewMatrix.setValue(viewMatrix)
				uniformModelMatrix.setValue(modelMatrix)

				model.drawMe
			}
			Display.update
			Display.sync(60)
		}

		model.release
		teardownGLDisplay
	}

	def setupModel = {
		type ModelFormat = VertexPositionFormat :~: VertexColorFormat
		val hex = VertexBuffer.allocate[ModelFormat](14)

		// Set up a hex column: 6 corners, including a center point
		val pio3 = Math.PI / 3
		val p3x = Math.cos(pio3).toFloat
		val p3y = Math.sin(pio3).toFloat

		val h = -4f

		val colors = Map[Int, (Float, Float, Float, Float)](
			0 -> (1, 1, 1, 1), // white
			1 -> (.5f, 0, .5f, 1), // purple
			2 -> (1, 0, 0, 1), // red
			3 -> (1, .64f, 0, 1), // orange
			4 -> (1, 1, 0, 1), // yellow
			5 -> (0, .5f, 0, 1), // green
			6 -> (0, 0, 1, 1)) // blue

		val xys = Map[Int, (Float, Float)](
			0 -> (0, 0), // center
			1 -> (1, 0), // right corner
			2 -> (p3x, p3y), // upper right
			3 -> (-p3x, p3y), // upper left
			4 -> (-1, 0), // left corner
			5 -> (-p3x, -p3y), // lower left
			6 -> (p3x, -p3y)) // lower right

		for (i <- 0 to 6) {
			val color = colors(i)
			val (x, y) = xys(i)
			hex.setXYZ(i, x, y, 0)
			hex.setXYZ(i + 7, x, y, h)
			hex.setRGBA(i, color)
			hex.setRGBA(i + 7, (0, 0, 0, 1))
		}

		val indices = Array[Byte](
			// TOP HEX
			0, 1, 2, // upper right triangle
			0, 2, 3, // upper triangle
			0, 3, 4, // upper left triangle
			0, 4, 5, // lower left triangle
			0, 5, 6, // lower triangle
			0, 6, 1, // lower right triangle

			// SIDES
			6, 13, 8, 6, 8, 1, // lower right side face
			5, 12, 13, 5, 13, 6, // lower side face
			4, 11, 12, 4, 12, 5, // lower left side face
			3, 10, 11, 3, 11, 4, // upper left side face
			2, 9, 10, 2, 10, 3, // upper side face
			1, 8, 9, 1, 9, 2, // upper right side face

			// BOTTOM FACE
			7, 8, 9, // upper right triangle
			7, 9, 10, // upper triangle
			7, 10, 11, // upper left triangle
			7, 11, 12, // lower left triangle
			7, 12, 13, // lower triangle
			7, 13, 8 // lower right triangle
			)
		val indexBuffer = BufferUtils.createByteBuffer(indices.length)
		indexBuffer.put(indices).flip

		val vertArrayHandle = GLVertexArrayHandle.createNew
		vertArrayHandle.bind {
			val vboInterleaved = GLBufferHandle.createNew
			vboInterleaved.bind(GL15.GL_ARRAY_BUFFER) {
				GL15.glBufferData(GL15.GL_ARRAY_BUFFER, hex.bytes, GL15.GL_STATIC_DRAW)
				println(getVertexAttributePointers[ModelFormat](0))
				hex.setVertexAttribPointers(0)
			}
		}

		val vertIndexHandle = GLBufferHandle.createNew
		vertIndexHandle.bind(GL15.GL_ELEMENT_ARRAY_BUFFER) {
			GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL15.GL_STATIC_DRAW)
		}

		ModelBone(vertArrayHandle, vertIndexHandle, indices.length)
	}

}

case class ModelBone(
	vertexArray: GLVertexArrayHandle,
	vertexIndices: GLBufferHandle,
	numIndices: Int) {

	def drawMe = vertexArray.bind {
		GL20.glEnableVertexAttribArray(0)
		GL20.glEnableVertexAttribArray(1)
		vertexIndices.bind(GL15.GL_ELEMENT_ARRAY_BUFFER) {
			GL11.glDrawElements(GL11.GL_TRIANGLES, numIndices, GL11.GL_UNSIGNED_BYTE, 0)
		}
		GL20.glDisableVertexAttribArray(0)
		GL20.glDisableVertexAttribArray(1)
	}

	def release = {
		vertexIndices.release
		vertexArray.release
	}
}