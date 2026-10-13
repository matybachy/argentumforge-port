package matybachy.argentumforge.client.mesh;

import matybachy.argentumforge.client.compat.GpuBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;

/** LWJGL 2 VBO wrapper for x,y,z,u,v,r,g,b,a vertices. */
public final class GlVbo implements GpuBuffer {
    private static final int FLOATS_PER_VERTEX = 9;
    private static final int BYTES_PER_FLOAT = 4;
    private int vboId;
    private int vertexCount;

    @Override
    public void upload(float[] vertices) {
        delete();
        if (vertices == null || vertices.length == 0) return;
        if (vertices.length % FLOATS_PER_VERTEX != 0) {
            throw new IllegalArgumentException("Invalid terrain vertex buffer length");
        }

        vertexCount = vertices.length / FLOATS_PER_VERTEX;
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void draw() {
        if (vboId == 0 || vertexCount == 0) return;
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        int stride = FLOATS_PER_VERTEX * BYTES_PER_FLOAT;
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, stride, 0L);
        GL11.glTexCoordPointer(2, GL11.GL_FLOAT, stride, 3L * BYTES_PER_FLOAT);
        GL11.glColorPointer(4, GL11.GL_FLOAT, stride, 5L * BYTES_PER_FLOAT);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

    @Override
    public void delete() {
        if (vboId != 0) {
            try {
                GL15.glDeleteBuffers(vboId);
            } catch (Throwable ignored) {
            }
        }
        vboId = 0;
        vertexCount = 0;
    }
}
