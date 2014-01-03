package io.dylemma.ui

trait MyShaders {
	def setupShaderProgram = {
		val vs = Shader.loadVertexShader {
			"""#version 150 core
			
			uniform mat4 projectionMatrix;
			uniform mat4 viewMatrix;
			uniform mat4 modelMatrix;
			
			in vec4 in_Position;
			in vec4 in_Color;
			
			out vec4 pass_Color;
			
			void main(void) {
				gl_Position = in_Position;
				// Override gl_Position with our new calculated position
				gl_Position = projectionMatrix * viewMatrix * modelMatrix * in_Position;
				
				pass_Color = in_Color;
			}"""
		}

		val fs = Shader.loadFragmentShader {
			"""#version 150 core
			|
			|in vec4 pass_Color;
			|
			|out vec4 out_Color;
			|
			|void main(void) {
			|	out_Color = pass_Color;
			|}""".stripMargin
		}

		// NOTE: the ShaderAttributes are optional: you only need them if things are out of order,
		// e.g. the Position attribute is bound to buffer 1, but appears at index 0 in the shader source
		ShaderProgram.createFrom(vs, fs, ShaderAttribute(0, "in_Position"), ShaderAttribute(1, "in_Color"))
	}
}