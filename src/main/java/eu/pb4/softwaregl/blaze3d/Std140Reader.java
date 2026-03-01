//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package eu.pb4.softwaregl.blaze3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.joml.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Environment(EnvType.CLIENT)
public class Std140Reader {
    private final ByteBuffer buffer;
    private final int start;

    private Std140Reader(final ByteBuffer buffer) {
        this.buffer = buffer;
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.start = buffer.position();
    }

    public static Std140Reader wrap(final ByteBuffer buffer) {
        return new Std140Reader(buffer);
    }

    public Std140Reader align(final int alignment) {
        int position = this.buffer.position();
        this.buffer.position(this.start + Mth.roundToward(position - this.start, alignment));
        return this;
    }

    public float putFloat() {
        this.align(4);
        return this.buffer.getFloat();
    }

    public int putInt() {
        this.align(4);
        return this.buffer.get();
    }

    public Vector2f getVec2() {
        return getVec2(new Vector2f());
    }

    public Vector2f getVec2(Vector2f vec) {
        this.align(8);

        vec.set(
                this.buffer.getFloat(),
                this.buffer.getFloat()
        );
        return vec;
    }

    public Vector2i getIVec2(Vector2i vec) {
        this.align(8);
        vec.set(this.buffer.getInt(), this.buffer.getInt());
        return vec;
    }

    public Vector3f getVec3() {
        return getVec3(new Vector3f());
    }
    public Vector3f getVec3(Vector3f vec) {
        this.align(16);
        vec.set(this.buffer.getFloat(), this.buffer.getFloat(), this.buffer.getFloat());
        this.buffer.position(this.buffer.position() + 4);
        return vec;
    }

    public Vector3i getIVec3() {
        return getIVec3(new Vector3i());
    }

    public Vector3i getIVec3(Vector3i vec) {
        this.align(16);
        vec.set(this.buffer.getInt(), this.buffer.getInt(), this.buffer.getInt());
        this.buffer.position(this.buffer.position() + 4);
        return vec;
    }


    public Vector4f getVec4() {
        return getVec4(new Vector4f());
    }
    public Vector4f getVec4(Vector4f vec) {
        this.align(16);

        vec.set(
                this.buffer.getFloat(),
                this.buffer.getFloat(),
                this.buffer.getFloat(),
                this.buffer.getFloat()
        );

        return vec;
    }

    public Vector4i getIVec4() {
        return getIVec4(new Vector4i());
    }
    public Vector4i getIVec4(Vector4i vec) {
        this.align(16);
        vec.set(
                this.buffer.getInt(),
                this.buffer.getInt(),
                this.buffer.getInt(),
                this.buffer.getInt()
        );
        return vec;
    }

    public Matrix4f getMat4f( Matrix4f vec) {
        this.align(16);
        vec.set(this.buffer);
        this.buffer.position(this.buffer.position() + 64);
        return vec;
    }

    public void reset() {
        this.buffer.position(this.start);
    }
}
