#ifdef GL_ES
	#define LOWP lowp
	precision mediump float;
#else
	#define LOWP
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
	vec4 col = texture2D(u_texture, v_texCoords);
	if(col.a == 0.0) 
		discard;
		
	gl_FragColor = col;
}