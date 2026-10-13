package matybachy.argentumforge.client.mesh;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

/**
 * Simple GL VBO wrapper using LWJGL 2 (compatible with Minecraft 1.8.9).
 * Manages a single VBO containing interleaved vertex attributes: x,y,z,r,g,b,a
 */
public final class GlVbo {
    private int vboId = 0;
    private int vertexCount = 0;

    public void upload(float[] vertices) {
        delete();
        vertexCount = vertices.length / 7; // x,y,z + r,g,b,a
        FloatBuffer buf = BufferUtils.createFloatBuffer(vertices.length);
        buf.put(vertices);
        buf.flip();

        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buf, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void draw() {
        if (vboId == 0 || vertexCount == 0) return;

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        int stride = 7 * 4; // 7 floats * 4 bytes

        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);

        // position pointer: 3 floats at offset 0
        GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, 0L);
        // color pointer: 4 floats at offset 12
        GL11.glColorPointer(4, GL11.GL_FLOAT, stride, 3 * 4);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);

        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    public void delete() {
        if (vboId != 0) {
            try {
                GL15.glDeleteBuffers(vboId);
            } catch (Throwable ignored) {}
            vboId = 0;
            vertexCount = 0;
        }
    }
}
