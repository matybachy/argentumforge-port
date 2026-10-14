package matybachy.argentumforge.client.mesh;

import java.util.Arrays;

/** Growable primitive float buffer used by the overlay meshers instead of boxed {@code List<Float>}. */
final class FloatArrayList {
    private float[] data;
    private int size;

    FloatArrayList() {
        this(1024);
    }

    FloatArrayList(int initialCapacity) {
        data = new float[Math.max(16, initialCapacity)];
    }

    void add(float value) {
        if (size == data.length) data = Arrays.copyOf(data, data.length << 1);
        data[size++] = value;
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    float[] toArray() {
        return Arrays.copyOf(data, size);
    }
}
