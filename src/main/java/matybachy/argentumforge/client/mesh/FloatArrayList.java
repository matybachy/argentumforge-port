package matybachy.argentumforge.client.mesh;

import java.util.Arrays;

/**
 * A growable primitive float array buffer to eliminate GC boxing overhead during meshing.
 */
public final class FloatArrayList {
    private float[] data;
    private int size;

    public FloatArrayList() {
        this(256);
    }

    public FloatArrayList(int initialCapacity) {
        this.data = new float[initialCapacity];
        this.size = 0;
    }

    public void add(float value) {
        if (size == data.length) {
            ensureCapacity(size + (size >> 1) + 16);
        }
        data[size++] = value;
    }

    public void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) {
            data = Arrays.copyOf(data, Math.max(data.length * 2, minCapacity));
        }
    }

    public int size() {
        return size;
    }

    public float get(int index) {
        return data[index];
    }

    public float[] toArray() {
        if (size == 0) return new float[0];
        return Arrays.copyOf(data, size);
    }

    public void clear() {
        size = 0;
    }
}
