package eu.pb4.potato3d;

import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

public class RGBA {
    private static final float[] FLOAT_LOOKUP = new float[256];
    public static int alpha(final int color) {
        return color & 255;
    }

    public static int red(final int color) {
        return (color >>> 24) & 255;
    }

    public static int green(final int color) {
        return (color >> 16) & 255;
    }

    public static int blue(final int color) {
        return (color >> 8) & 255;
    }

    public static int colorARGB(final int alpha, final int red, final int green, final int blue) {
        return (alpha & 255) | (red & 255) << 24 | (green & 255) << 16 | (blue & 255) << 8;
    }

    public static int colorARGB(final int color) {
        return (color & 0xFFFFFF) << 8 | ARGB.alpha(color);
    }

    public static int color(final int red, final int green, final int blue) {
        return colorARGB(255, red, green, blue);
    }

    public static int color(final Vec3 vec) {
        return color(as8BitChannel((float)vec.x()), as8BitChannel((float)vec.y()), as8BitChannel((float)vec.z()));
    }

    public static int multiply(final int lhs, final int rhs) {
        if (lhs == -1) {
            return rhs;
        } else {
            return rhs == -1 ? lhs : colorARGB(alpha(lhs) * alpha(rhs) / 255, red(lhs) * red(rhs) / 255, green(lhs) * green(rhs) / 255, blue(lhs) * blue(rhs) / 255);
        }
    }

    public static int addRgb(final int lhs, final int rhs) {
        return colorARGB(alpha(lhs), Math.min(red(lhs) + red(rhs), 255), Math.min(green(lhs) + green(rhs), 255), Math.min(blue(lhs) + blue(rhs), 255));
    }

    public static int subtractRgb(final int lhs, final int rhs) {
        return colorARGB(alpha(lhs), Math.max(red(lhs) - red(rhs), 0), Math.max(green(lhs) - green(rhs), 0), Math.max(blue(lhs) - blue(rhs), 0));
    }

    public static int multiplyAlpha(final int color, final float alphaMultiplier) {
        if (color != 0 && !(alphaMultiplier <= 0.0F)) {
            return alphaMultiplier >= 1.0F ? color : colorRGBA(alphaFloat(color) * alphaMultiplier, color);
        } else {
            return 0;
        }
    }

    private static int colorRGBA(float alpha, int rgba) {
        return (rgba & 0xFFFFFF00) | as8BitChannel(alpha);
    }

    public static int grey(final int color) {
        return colorARGB(alpha(color), color, color, color);
    }

    public static int alphaBlend(final int destination, final int source) {
        int destinationAlpha = alpha(destination);
        int sourceAlpha = alpha(source);
        if (sourceAlpha == 255) {
            return source;
        } else if (sourceAlpha == 0) {
            return destination;
        } else {
            int alpha = sourceAlpha + destinationAlpha * (255 - sourceAlpha) / 255;
            return colorARGB(alpha, alphaBlendChannel(alpha, sourceAlpha, red(destination), red(source)), alphaBlendChannel(alpha, sourceAlpha, green(destination), green(source)), alphaBlendChannel(alpha, sourceAlpha, blue(destination), blue(source)));
        }
    }

    private static int alphaBlendChannel(final int resultAlpha, final int sourceAlpha, final int destination, final int source) {
        return (source * sourceAlpha + destination * (resultAlpha - sourceAlpha)) / resultAlpha;
    }

    public static int as8BitChannel(final float value) {
        return Mth.floor(value * 255.0F) & 0xFF;
    }

    public static float alphaFloat(final int color) {
        return from8BitChannel(alpha(color));
    }

    public static float redFloat(final int color) {
        return from8BitChannel(red(color));
    }

    public static float greenFloat(final int color) {
        return from8BitChannel(green(color));
    }

    public static float blueFloat(final int color) {
        return from8BitChannel(blue(color));
    }

    private static float from8BitChannel(final int value) {
        return value / 255.0F;
    }

    public static int colorFromFloatRGBA(float red, float green, float blue, float alpha) {
        return ((int) (red * 0xFF) & 0xFF) << 24
                | ((int) (green * 0xFF) & 0xFF) << 16
                | ((int) (blue * 0xFF) & 0xFF) << 8
                | ((int) (alpha * 0xFF) & 0xFF);
    }

    public static int fromVector4f(Vector4f color) {
        return ((int) (color.x * 0xFF) & 0xFF) << 24
                | ((int) (color.y * 0xFF) & 0xFF) << 16
                | ((int) (color.z * 0xFF) & 0xFF) << 8
                | ((int) (color.w * 0xFF) & 0xFF);
    }

    public static Vector4f toVector4f(int color, Vector4f out) {
        return out.set(FLOAT_LOOKUP[(color >> 24) & 0xFF], FLOAT_LOOKUP[(color >> 16) & 0xFF], FLOAT_LOOKUP[(color >> 8) & 0xFF], FLOAT_LOOKUP[color & 0xFF]);
    }

    static {
        for (int i = 0; i < 256; i++) {
            FLOAT_LOOKUP[i] = i / 255f;
        }
    }
}
