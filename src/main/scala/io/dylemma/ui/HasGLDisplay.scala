package io.dylemma.ui

import org.lwjgl.opengl.PixelFormat
import org.lwjgl.opengl.ContextAttribs
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.lwjgl.opengl.GL11

trait HasGLDisplay {

	protected var displayClearColor = (.4f, .6f, .9f, 0f)
	protected var displayWidth = 1600
	protected var displayHeight = 900
	protected var displayTitle = "OpenGL Demo"

	def setupGLDisplay = {
		val pixelFormat = new PixelFormat
		val contextAttribs = new ContextAttribs(3, 2).withProfileCore(true).withForwardCompatible(true)
		Display.setDisplayMode(new DisplayMode(displayWidth, displayHeight))
		Display.setTitle(displayTitle)
		Display.create(pixelFormat, contextAttribs)

		displayClearColor match {
			case (r, g, b, a) => GL11.glClearColor(r, g, b, a)
		}
		GL11.glViewport(0, 0, displayWidth, displayHeight)
	}

	def teardownGLDisplay = {
		Display.destroy()
	}
}