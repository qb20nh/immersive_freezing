#version 150

out vec2 texCoord0;

// Fullscreen triangle post-process vertex shader.
// Uses gl_VertexID because the pipeline uses DefaultVertexFormat.EMPTY.
void main() {
    vec2 pos;
    if (gl_VertexID == 0) {
        pos = vec2(-1.0, -1.0);
    } else if (gl_VertexID == 1) {
        pos = vec2(3.0, -1.0);
    } else {
        pos = vec2(-1.0, 3.0);
    }

    texCoord0 = (pos + 1.0) * 0.5;
    gl_Position = vec4(pos, 0.0, 1.0);
}


