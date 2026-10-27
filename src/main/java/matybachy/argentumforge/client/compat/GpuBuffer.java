package matybachy.argentumforge.client.compat;

public interface GpuBuffer {
    void upload(float[] vertices);

    void draw();

    void delete();
}
