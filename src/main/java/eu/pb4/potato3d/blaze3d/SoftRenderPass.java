package eu.pb4.potato3d.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.GpuQueryPool;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import eu.pb4.potato3d.RGBA;
import eu.pb4.potato3d.blaze3d.shader.EndShader;
import eu.pb4.potato3d.blaze3d.shader.SampledTexture;
import eu.pb4.potato3d.blaze3d.shader.SoftShader;
import eu.pb4.potato3d.blaze3d.texture.DepthTexture;
import eu.pb4.potato3d.blaze3d.texture.RGBATexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import org.joml.*;
import org.jspecify.annotations.Nullable;

import java.lang.Math;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

public class SoftRenderPass implements RenderPassBackend {
    public static final boolean USE_ZERO_TO_ONE_Z = true;
    private static final float CLIP_Z_MIN = USE_ZERO_TO_ONE_Z ? 0 : -1;

    private final SoftCommandEncoder encoder;
    private final SoftTextureView colorTexture;
    private final @Nullable SoftTextureView depthTexture;
    private final Map<String, SampledTexture> binds = new HashMap<>();
    private final Map<String, Std140Reader> uniforms = new HashMap<>();
    private final ByteBuffer[] vertexBuffers = new ByteBuffer[1];

    private final Vector4f[] pos;
    private final Vector2f[] uvs;
    private final Vector4f[] colors;
    private final Vector3f[] normals;
    private final float[] lineWidth;

    private boolean closed;
    private RenderPipeline pipeline;
    private ByteBuffer indexBuffer;
    private VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;
    private Scissor scissor;
    private DepthTestPredicate depthTest;
    private ColorBlender blender;
    private boolean writeDepth;
    private boolean limitDepth;
    private float depthBiasConstant;
    private float depthBiasScaleFactor;
    private int vertexLength;
    private int positionOffset;
    private int colorOffset;
    private int uvOffset;
    private int uv1Offset;
    private int uv2Offset;
    private int normalOffset;
    private int lineWidthOffset;
    private boolean isGlint;
    private DrawCall drawCall = this::executeDraw;

    public SoftRenderPass(SoftCommandEncoder encoder, String label, SoftTextureView colorTexture, @Nullable SoftTextureView depthTexture) {
        this.encoder = encoder;
        this.colorTexture = colorTexture;
        this.scissor = null;
        this.depthTexture = depthTexture;

        this.pos = new Vector4f[]{
                new Vector4f(), new Vector4f(), new Vector4f(),
                new Vector4f(), new Vector4f(), new Vector4f(),
                new Vector4f(), new Vector4f(), new Vector4f(),
                new Vector4f(), new Vector4f(), new Vector4f(),
        };

        this.uvs = new Vector2f[]{
                new Vector2f(), new Vector2f(), new Vector2f(),
                new Vector2f(), new Vector2f(), new Vector2f(),
                new Vector2f(), new Vector2f(), new Vector2f(),
                new Vector2f(), new Vector2f(), new Vector2f(),
        };

        this.colors = new Vector4f[]{
                new Vector4f(), new Vector4f(), new Vector4f(),
                new Vector4f(), new Vector4f(), new Vector4f(),
                new Vector4f(), new Vector4f(), new Vector4f(),
                new Vector4f(), new Vector4f(), new Vector4f(),
        };

        this.normals = new Vector3f[]{
                new Vector3f(), new Vector3f(), new Vector3f(),
                new Vector3f(), new Vector3f(), new Vector3f(),
                new Vector3f(), new Vector3f(), new Vector3f(),
                new Vector3f(), new Vector3f(), new Vector3f(),
        };

        this.lineWidth = new float[16];
    }

    private void lerp(int a, int b, int out, float frec) {
        pos[a].lerp(pos[b], frec, pos[out]);
        uvs[a].lerp(uvs[b], frec, uvs[out]);
        colors[a].lerp(colors[b], frec, colors[out]);
        normals[a].lerp(normals[b], frec, normals[out]);
        lineWidth[out] = Mth.lerp(frec, lineWidth[a], lineWidth[b]);

    }

    private void copy(int a, int out) {
        pos[out].set(pos[a]);
        uvs[out].set(uvs[a]);
        colors[out].set(colors[a]);
        normals[out].set(normals[a]);
        lineWidth[out] = lineWidth[a];
    }

    @Override
    public void pushDebugGroup(Supplier<String> label) {

    }

    @Override
    public void popDebugGroup() {

    }

    @Override
    public void bindTexture(String name, @Nullable GpuTextureView textureView, @Nullable GpuSampler sampler) {
        this.binds.put(name, new SampledTexture((SoftTextureView) textureView, (SoftSampler) sampler));
    }

    @Override
    public void setUniform(String name, GpuBuffer value) {
        this.uniforms.put(name, Std140Reader.wrap(((SoftBuffer) value).data()));
    }

    @Override
    public void setUniform(String name, GpuBufferSlice value) {
        var buf = ((SoftBuffer) value.buffer()).data().slice((int) value.offset(), (int) value.length());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        this.uniforms.put(name, Std140Reader.wrap(buf));
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        this.scissor = new Scissor(x, y, x + width, y + height);
    }

    @Override
    public void disableScissor() {
        this.scissor = null;
    }

    @Override
    public void setVertexBuffer(int slot, GpuBuffer vertexBuffer) {
        this.vertexBuffers[slot] = ((SoftBuffer) vertexBuffer).data();
    }

    @Override
    public void setIndexBuffer(GpuBuffer indexBuffer, VertexFormat.IndexType indexType) {
        this.indexBuffer = ((SoftBuffer) indexBuffer).data();
        this.indexType = indexType;
    }

    @Override
    public void drawIndexed(int baseVertex, int firstIndex, int indexCount, int instanceCount) {
        try {
            this.drawCall.draw(this.vertexBuffers[0], this.indexBuffer, baseVertex, firstIndex, indexCount, this.indexType, instanceCount, this.uniforms);
        } catch (Throwable e) {
            System.currentTimeMillis();
            // Suffering
        }
    }

    @Override
    public <T> void drawMultipleIndexed(Collection<RenderPass.Draw<T>> draws, @Nullable GpuBuffer defaultIndexBuffer, VertexFormat.@Nullable IndexType defaultIndexType, Collection<String> dynamicUniforms, T uniformArgument) {
        try {
            for (var draw : draws) {
                var indexBuf = draw.indexBuffer() != null ? draw.indexBuffer() : defaultIndexBuffer;
                var indexType = draw.indexType() != null ? draw.indexType() : defaultIndexType;
                var x = new HashMap<>(this.uniforms);

                draw.uniformUploaderConsumer().accept(uniformArgument, (name, buffer) -> x.put(name, Std140Reader.wrap(((SoftBuffer) buffer.buffer()).data().slice((int) buffer.offset(), (int) buffer.length()))));
                this.drawCall.draw(((SoftBuffer) draw.vertexBuffer()).data(), indexBuf != null ? ((SoftBuffer) indexBuf).data() : null, draw.baseVertex(), draw.firstIndex(), draw.indexCount(), indexType, 1, x);
            }
        } catch (Throwable e) {
            System.currentTimeMillis();
            // Suffering
        }
    }

    @Override
    public void draw(int firstVertex, int vertexCount) {
        try {
            this.drawCall.draw(this.vertexBuffers[0], null, firstVertex, 0, vertexCount, null, 1, this.uniforms);
        } catch (Throwable e) {
            System.currentTimeMillis();
            // Pain
        }
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {

    }

    @Override
    public void setPipeline(RenderPipeline pipeline) {
        this.pipeline = pipeline;

        var vertexFormat = this.pipeline.getVertexFormat();

        this.vertexLength = vertexFormat.getVertexSize();
        this.positionOffset = vertexFormat.getOffset(VertexFormatElement.POSITION);
        this.colorOffset = vertexFormat.getOffset(VertexFormatElement.COLOR);
        this.uvOffset = vertexFormat.getOffset(VertexFormatElement.UV0);
        this.uv1Offset = vertexFormat.getOffset(VertexFormatElement.UV1);
        this.uv2Offset = vertexFormat.getOffset(VertexFormatElement.UV2);
        this.normalOffset = vertexFormat.getOffset(VertexFormatElement.NORMAL);
        this.lineWidthOffset = vertexFormat.getOffset(VertexFormatElement.LINE_WIDTH);

        this.isGlint = this.pipeline.getColorTargetState().blendFunction().isPresent() && this.pipeline.getColorTargetState().blendFunction().get() == BlendFunction.GLINT;


        var depthStencilState = pipeline.getDepthStencilState();
        this.writeDepth = depthStencilState != null && depthStencilState.writeDepth();
        this.limitDepth = depthStencilState != null && depthStencilState.depthTest() != CompareOp.ALWAYS_PASS;
        //this.depthBiasConstant = depthStencilState != null ? depthStencilState.depthBiasConstant() * 1.0E-20F : 0;
        //this.depthBiasScaleFactor = depthStencilState != null ? depthStencilState.depthBiasScaleFactor() : 0;
        this.depthBiasConstant = depthStencilState != null ? depthStencilState.depthBiasScaleFactor() * Mth.EPSILON * 10 : 0;
        this.depthTest = depthStencilState != null ? switch (depthStencilState.depthTest()) {
            case ALWAYS_PASS -> (image, drawn) -> true;
            case EQUAL -> (image, drawn) -> image == drawn;
            case NOT_EQUAL -> (image, drawn) -> image != drawn;
            case LESS_THAN_OR_EQUAL -> (image, drawn) -> image >= drawn;
            case LESS_THAN -> (image, drawn) -> image > drawn;
            case GREATER_THAN -> (image, drawn) -> image < drawn;
            case GREATER_THAN_OR_EQUAL -> (image, drawn) -> image <= drawn;
            case NEVER_PASS -> (image, drawn) -> false;
        } : (a, b) -> true;

        var colorState = pipeline.getColorTargetState();
        if (pipeline == RenderPipelines.SOLID_TERRAIN) {
            this.blender = ColorBlender.SOLID;
        } else if (pipeline == RenderPipelines.CUTOUT_TERRAIN) {
            this.blender = ColorBlender.CUTOUT;
        } else if (colorState.blendFunction().isEmpty()) {
            this.blender = ColorBlender.CUTOUT;
        } else if (colorState.blendFunction().get() == BlendFunction.INVERT) {
            this.blender = ColorBlender.INVERT;
        } else if (colorState.blendFunction().get() == BlendFunction.ADDITIVE) {
            this.blender = ColorBlender.ADDITIVE;
        } else if (colorState.blendFunction().get() == BlendFunction.OVERLAY) {
            this.blender = ColorBlender.OVERLAY;
        } else if (colorState.blendFunction().get() == BlendFunction.GLINT) {
            this.blender = ColorBlender.OVERLAY;
        } else if (colorState.blendFunction().get() == BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA) {
            this.blender = ColorBlender.PREMULTIPLIED_BLEND;
        } else if (colorState.blendFunction().get() == BlendFunction.TRANSLUCENT) {
            this.blender = ColorBlender.ALPHA_BLEND;
        } else if (pipeline == RenderPipelines.CRUMBLING) {
            this.blender = ColorBlender.MULTIPLY;
        } else {
            this.blender = ColorBlender.PREMULTIPLIED_BLEND;
        }

        if (this.pipeline == RenderPipelines.ANIMATE_SPRITE_BLIT || this.pipeline == RenderPipelines.ANIMATE_SPRITE_INTERPOLATE) {
            this.drawCall = this::drawAnimateSpriteBlit;
        } else if (this.pipeline == RenderPipelines.LIGHTMAP) {
            this.drawCall = this::executeLightmapDraw;
        } else if (this.pipeline == RenderPipelines.VIGNETTE) {
            this.drawCall = this::executeNoOpDraw;
        } else {
            this.drawCall = this::executeDraw;
        }
    }


    private void drawAnimateSpriteBlit(ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer, int baseVertex, int firstIndex, int drawCount, VertexFormat.@Nullable IndexType indexType, int instanceCount, Map<String, Std140Reader> uniforms) {
        if (!(this.uniforms.get("SpriteAnimationInfo") instanceof Std140Reader reader)) {
            return;
        }

        final var maxProgress = 1000;
        var progress = baseVertex >> 3;
        var invProgress = maxProgress - progress;

        var output = (SoftTexture) (RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride.texture() : this.colorTexture.texture());
        var halfWidth = output.getWidth(0) / 2;
        var halfHeight = output.getHeight(0) / 2;
        var projMat = reader.getMat4f(new Matrix4f()).mul(reader.getMat4f(new Matrix4f()));
        var xPadding = reader.putFloat();
        var yPadding = reader.putFloat();
        var mipmapLevels = reader.putInt();
        var min = projMat.transformProject(new Vector3f(0)).mul(halfWidth, halfHeight, 1).add(halfWidth, halfHeight, 0);
        var max = projMat.transformProject(new Vector3f(1)).mul(halfWidth, halfHeight, 1).add(halfWidth, halfHeight, 0);

        int minX = (int) Mth.clamp(min.x, 0, halfWidth * 2 - 1);
        int minY = (int) Mth.clamp(min.y, 0, halfHeight * 2 - 1);

        int maxX = (int) Mth.clamp(max.x, 0, halfWidth * 2 - 1);
        int maxY = (int) Mth.clamp(max.y, 0, halfHeight * 2 - 1);

        var lengthX = (maxX - minX);
        var lengthY = (maxY - minY);


        if (this.pipeline == RenderPipelines.ANIMATE_SPRITE_BLIT) {
            SampledTexture sprite = this.binds.get("Sprite");

            var spriteWidth = sprite.texture().getWidth(0);
            var spriteHeight = sprite.texture().getHeight(0);
            var margin = (lengthX - spriteWidth) / 2;

            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    output.setRGBA(0, x, y, sprite.sample(
                            x, y,
                            (float) (x - minX - margin) / spriteWidth,
                            (float) (y - minY - margin) / spriteHeight,
                            0, 0
                    ));
                }
            }

        } else if (this.pipeline == RenderPipelines.ANIMATE_SPRITE_INTERPOLATE) {
            SampledTexture currentSprite = this.binds.get("CurrentSprite");
            SampledTexture nextSprite = this.binds.get("NextSprite");

            var spriteWidth = currentSprite.texture().getWidth(0);
            var spriteHeight = currentSprite.texture().getHeight(0);

            var margin = (lengthX - spriteWidth) / 2;

            for (int x = minX; x < maxX; x++) {
                for (int y = minY; y < maxY; y++) {
                    var u = (float) (x - minX - margin) / spriteWidth;
                    var v = (float) (y - minY - margin) / spriteHeight;

                    var curr = currentSprite.sample(x, y, u, v, 0, 0);
                    var next = nextSprite.sample(x, y, u, v, 0, 0);

                    output.setRGBA(0, x, y, RGBA.colorARGB(
                            (RGBA.alpha(curr) * invProgress + RGBA.alpha(next) * progress) / maxProgress,
                            (RGBA.red(curr) * invProgress + RGBA.red(next) * progress) / maxProgress,
                            (RGBA.green(curr) * invProgress + RGBA.green(next) * progress) / maxProgress,
                            (RGBA.blue(curr) * invProgress + RGBA.blue(next) * progress) / maxProgress
                    ));
                }
            }

        }
    }

    private void executeLightmapDraw(ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer, int baseVertex, int firstIndex, int drawCount, VertexFormat.@Nullable IndexType indexType, int instanceCount, Map<String, Std140Reader> uniforms) {
        if (!(this.uniforms.get("LightmapInfo") instanceof Std140Reader reader)) {
            return;
        }

        var output = (SoftTexture) (RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride.texture() : this.colorTexture.texture());
        float skyFactor = reader.putFloat();
        float blockFactor = reader.putFloat();
        float NightVisionFactor = reader.putFloat();
        float DarknessScale = reader.putFloat();
        float BossOverlayWorldDarkeningFactor = reader.putFloat();
        float BrightnessFactor = reader.putFloat();
        var blockLightTint = reader.getVec3();
        var skyLightColor = reader.getVec3();
        var AmbientColor = reader.getVec3();
        var NightVisionColor = reader.getVec3();
        reader.reset();

        var color = new Vector3f();
        var tmp = new Vector3f();

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                tmp.set(0);
                color.set(0);
                float blockLevel = x / 15f;
                float skyLevel = y / 15f;

                var blockBrightness = blockFactor * blockLevel / (4.0f - 3.0f * blockLevel);
                var skyBrightness = skyFactor * skyLevel / (4.0f - 3.0f * skyLevel);


                // Calculate ambient color with or without night vision
                color.set(NightVisionColor).mul(NightVisionFactor);
                color.max(AmbientColor);

                // Add sky light
                color.add(tmp.set(skyLightColor).mul(skyBrightness));

                // Add block light
                var parabolicFactor = 0.9f * (2.0f * blockLevel - 1.0f) * (2.0f * blockLevel - 1.0f);
                tmp.set(blockLightTint).lerp(new Vector3f(1), parabolicFactor);
                color.add(tmp.mul(blockBrightness));

                // Apply boss overlay darkening effect
                color.lerp(tmp.set(color).mul(0.7f, 0.6f, 0.6f), BossOverlayWorldDarkeningFactor);

                // Apply darkness effect scale
                color = color.sub(DarknessScale, DarknessScale, DarknessScale);

                // Apply brightness
                color = color.set(Math.clamp(color.x, 0, 1), Math.clamp(color.y, 0, 1), Math.clamp(color.z, 0, 1));

                // NotGamma
                {
                    float maxComponent = Math.max(Math.max(color.x, color.y), color.z);
                    float maxInverted = 1.0f - maxComponent;
                    float maxScaled = 1.0f - maxInverted * maxInverted * maxInverted * maxInverted;
                    tmp.set(color).mul(maxScaled / maxComponent);
                }

                color = color.lerp(tmp, BrightnessFactor);

                output.setRGBA(0, x, y, RGBA.colorFromFloatRGBA(
                        Mth.clamp(color.x, 0, 1),
                        Mth.clamp(color.y, 0, 1),
                        Mth.clamp(color.z, 0, 1),
                        1
                ));
            }
        }
    }

    private void executeNoOpDraw(ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer, int baseVertex, int firstIndex, int drawCount, VertexFormat.@Nullable IndexType indexType, int instanceCount, Map<String, Std140Reader> uniforms) {
    }

    private void executeDraw(ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer, int baseVertex, int firstIndex, int drawCount, VertexFormat.@Nullable IndexType indexType, int instanceCount, Map<String, Std140Reader> uniforms) {
        if (this.pipeline == RenderPipelines.END_PORTAL) {
            System.currentTimeMillis();
        }

        var projMat = new Matrix4f();
        var modelViewMat = new Matrix4f();
        var colorModulator = new Vector4f(1, 1, 1, 1);
        var modelOffset = new Vector3f();
        var textureMat = new Matrix4f();
        var cameraBlockPos = new Vector3i();
        var cameraOffset = new Vector3f();

        var position = new Vector3f();
        SoftShader sampler0 = this.pipeline.getSamplers().contains("Sampler0") ? this.binds.get("Sampler0") : null;
        SampledTexture sampler1 = this.pipeline.getSamplers().contains("Sampler1") ? this.binds.get("Sampler1") : null;
        SampledTexture sampler2 = this.pipeline.getSamplers().contains("Sampler2") ? this.binds.get("Sampler2") : null;

        var glintAlpha = 1f;
        var gameTime = 1f;

        if (uniforms.get("Globals") instanceof Std140Reader reader) {
            reader.getIVec3(cameraBlockPos);
            reader.getVec3(cameraOffset);
            reader.getVec2(); // ScreenSize
            glintAlpha = reader.putFloat();
            gameTime = reader.putFloat();
            reader.reset();
        }

        if (uniforms.get("Projection") instanceof Std140Reader reader) {
            reader.getMat4f(projMat);
            reader.reset();
        }

        if (uniforms.get("DynamicTransforms") instanceof Std140Reader reader) {
            reader.getMat4f(modelViewMat);
            reader.getVec4(colorModulator);
            reader.getVec3(modelOffset);
            reader.getMat4f(textureMat);
            reader.reset();
        }


        var hasLighting = false;
        Vector3f lightDir0 = new Vector3f();
        Vector3f lightDir1 = new Vector3f();
        if (uniforms.get("Lighting") instanceof Std140Reader reader) {
            reader.getVec3(lightDir0);
            reader.getVec3(lightDir1);
            reader.reset();
            hasLighting = true;
        }

        if (uniforms.get("ChunkSection") instanceof Std140Reader reader) {
            reader.getMat4f(modelViewMat);
            reader.putFloat(); // ChunkVisibility
            reader.getIVec2(new Vector2i()); // Texture Size
            var chunkPos = reader.getIVec3();

            position.set(
                    chunkPos.x - cameraBlockPos.x + cameraOffset.x,
                    chunkPos.y - cameraBlockPos.y + cameraOffset.y,
                    chunkPos.z - cameraBlockPos.z + cameraOffset.z
            );
            reader.reset();
        }

        var colorOutput = ((SoftTexture) (RenderSystem.outputColorTextureOverride != null ? RenderSystem.outputColorTextureOverride.texture() : this.colorTexture.texture())).rgba[0];
        var depthOutput = (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride.texture() : this.depthTexture != null ? this.depthTexture.texture() : null) instanceof SoftTexture softTexture ? softTexture.depth[0] : null;

        int width = colorOutput.width();
        int height = colorOutput.height();

        var halfWidth = width / 2f;
        var halfHeight = height / 2f;

        if (vertexBuffer == null) return;
        var indexStart = indexBuffer != null ? indexBuffer.position() + indexType.bytes * firstIndex : 0;
        var vertInit = vertexBuffer.position();

        Scissor scissor;
        if (this.scissor != null) {
            scissor = new Scissor(
                    Math.max(this.scissor.x1, 0), Math.max(this.scissor.y1, 0),
                    Math.min(this.scissor.x2, colorOutput.width()), Math.min(this.scissor.y2, colorOutput.height())
            );
        } else {
            scissor = new Scissor(0, 0, colorOutput.width(), colorOutput.height());
        }

        var vertexMode = this.pipeline.getVertexFormatMode();

        var uvOffset = sampler0 != null ? this.uvOffset : -1;
        var uv1Offset = sampler1 != null ? this.uv1Offset : -1;
        var uv2Offset = sampler2 != null ? this.uv2Offset : -1;

        if (this.pipeline == RenderPipelines.END_PORTAL) {
            sampler0 = new EndShader((SampledTexture) sampler0, 0x444444FF, (int) System.currentTimeMillis() / 2);
        } else if (this.pipeline == RenderPipelines.END_GATEWAY) {
            sampler0 = new EndShader((SampledTexture) sampler0, 0x555577FF, (int) System.currentTimeMillis() / 3);
        }

        var workMat = new Matrix4f();
        workMat.set(projMat).mul(modelViewMat);

        if (vertexMode == VertexFormat.Mode.QUADS || vertexMode == VertexFormat.Mode.TRIANGLES) {
            int layer = 0;
            int triangle = 0;

            for (int i = 0; i < drawCount; i += 3) {
                for (int a = 0; a < 3; a++) {
                    var pos = vertInit + getVertexPos(indexType, indexBuffer, indexStart, i + a, baseVertex) * vertexLength;

                    this.readVertexFull(
                            vertexBuffer,
                            position, colorModulator, workMat, textureMat, lightDir0, lightDir1,
                            sampler1, sampler2, vertexMode == VertexFormat.Mode.TRIANGLES || (triangle & 1) == 0, hasLighting,
                            uvOffset, uv1Offset, uv2Offset, pos, a
                    );

                    if (isGlint) {
                        colors[a].w *= glintAlpha;
                    }
                }

                int vertStart = 0;
                int actualVertCount = 3;

                if (pos[0].w <= 0.05f || pos[1].w <= 0.05f || pos[2].w <= 0.05f) {
                    actualVertCount = clipTriangleWFast(3) + 3;

                    vertStart = 3;
                }

                if (this.pipeline == RenderPipelines.PANORAMA) {
                    layer = switch (triangle / 2) {
                        case 0 -> 4; // B
                        case 1 -> 0; // C
                        case 2 -> 5; // D
                        case 3 -> 1; // A
                        case 4 -> 3; // UP
                        case 5 -> 2; // Down
                        default -> 0;
                    };
                }

                for (var t = vertStart; t < actualVertCount; t += 3) {
                    convertIntoScreenSpace(pos[t], halfWidth, halfHeight);
                    convertIntoScreenSpace(pos[t + 1], halfWidth, halfHeight);
                    convertIntoScreenSpace(pos[t + 2], halfWidth, halfHeight);

                    if (pos[t].z < CLIP_Z_MIN && pos[t + 1].z < CLIP_Z_MIN && pos[t + 2].z < CLIP_Z_MIN) {
                        continue;
                    }

                    drawTriangle(colorOutput, depthOutput, scissor, t, pos, uvs, colors, sampler0, layer);
                }

                triangle++;
            }
        } else if (vertexMode == VertexFormat.Mode.TRIANGLE_FAN) {
            var pos0 = vertInit + getVertexPos(indexType, indexBuffer, indexStart, 0, baseVertex) * vertexLength;
            var pos1 = vertInit + getVertexPos(indexType, indexBuffer, indexStart, 1, baseVertex) * vertexLength;

            this.readVertexFull(
                    vertexBuffer,
                    position, colorModulator, workMat, textureMat, lightDir0, lightDir1,
                    sampler1, sampler2, true, hasLighting,
                    uvOffset, uv1Offset, uv2Offset, pos0, 0
            );
            this.readVertexFull(
                    vertexBuffer,
                    position, colorModulator, workMat, textureMat, lightDir0, lightDir1,
                    sampler1, sampler2, true, hasLighting,
                    uvOffset, uv1Offset, uv2Offset, pos1, 1
            );

            for (int i = 2; i < drawCount; i += 1) {
                var pos = vertInit + getVertexPos(indexType, indexBuffer, indexStart, i, baseVertex) * vertexLength;

                this.readVertexFull(
                        vertexBuffer,
                        position, colorModulator, workMat, textureMat, lightDir0, lightDir1,
                        sampler1, sampler2, true, hasLighting,
                        uvOffset, uv1Offset, uv2Offset, pos, 2
                );

                int vertStart = 3;
                int actualVertCount = 6;

                if (this.pos[0].w <= 0.05f || this.pos[1].w <= 0.05f || this.pos[2].w <= 0.05f) {
                    actualVertCount = clipTriangleWFast(vertStart) + 3;
                } else {
                    copy(0, vertStart);
                    copy(1, vertStart + 1);
                    copy(2, vertStart + 2);
                }

                for (var t = vertStart; t < actualVertCount; t += 3) {
                    convertIntoScreenSpace(this.pos[t], halfWidth, halfHeight);
                    convertIntoScreenSpace(this.pos[t + 1], halfWidth, halfHeight);
                    convertIntoScreenSpace(this.pos[t + 2], halfWidth, halfHeight);

                    if (this.pos[t].z < CLIP_Z_MIN && this.pos[t + 1].z < CLIP_Z_MIN && this.pos[t + 2].z < CLIP_Z_MIN) {
                        continue;
                    }

                    drawTriangle(colorOutput, depthOutput, scissor, t, this.pos, uvs, colors, sampler0, 0);
                }
                copy(2, 1);
            }
        } else if (vertexMode == VertexFormat.Mode.LINES) {
            int layer = 0;

            for (int i = 0; i < drawCount; i += 2) {
                for (int a = 0; a < 2; a++) {
                    var pos = vertInit + getVertexPos(indexType, indexBuffer, indexStart, i + a, baseVertex) * vertexLength;

                    this.readVertexFull(
                            vertexBuffer,
                            position, colorModulator, workMat, textureMat, lightDir0, lightDir1,
                            sampler1, sampler2, true, false,
                            uvOffset, uv1Offset, uv2Offset, pos, a
                    );

                    if (isGlint) {
                        colors[a].w *= glintAlpha;
                    }
                }

                int vertStart = 0;
                int actualVertCount = 2;

                if (pos[0].w <= 0.05f || pos[1].w <= 0.05f) {
                    actualVertCount = clipLineWFast(2) + 2;
                    vertStart = 2;
                }

                for (var t = vertStart; t < actualVertCount; t += 2) {
                    convertIntoScreenSpace(pos[t], halfWidth, halfHeight);
                    convertIntoScreenSpace(pos[t + 1], halfWidth, halfHeight);

                    if (pos[t].z < CLIP_Z_MIN && pos[t + 1].z < CLIP_Z_MIN) {
                        continue;
                    }

                    drawLine(colorOutput, depthOutput, scissor, t, pos, uvs, colors, lineWidth, sampler0, layer);
                }
            }
        }
    }

    private int getVertexPos(VertexFormat.@Nullable IndexType indexType, @Nullable ByteBuffer indexBuffer, int indexStart, int i, int baseVertex) {
        return indexType == null
                ? i + baseVertex
                : indexType == VertexFormat.IndexType.INT
                ? indexBuffer.getInt(indexStart + i * 4) + baseVertex
                : indexBuffer.getShort(indexStart + i * 2) + baseVertex;
    }

    private void convertIntoScreenSpace(Vector4f vec, float halfWidth, float halfHeight) {
        vec.w = 1 / vec.w;
        vec.mul(halfWidth * vec.w, halfHeight * vec.w, vec.w, 1).add(halfWidth, halfHeight, 0, 0);
    }

    private void readVertexFull(ByteBuffer vertexBuffer,
                                Vector3f position, Vector4f colorModulator, Matrix4f workMat, Matrix4f textureMat,
                                Vector3f lightDir0, Vector3f lightDir1, @Nullable SampledTexture sampler1, @Nullable SampledTexture sampler2,
                                boolean triangleMode, boolean hasLighting,
                                int uvOffset, int uv1Offset, int uv2Offset,
                                int pos, int a) {
        var tmpVec4f = this.pos[this.pos.length - 1];

        if (positionOffset != -1) {
            this.pos[a].set(
                    vertexBuffer.getFloat(pos + positionOffset) + position.x,
                    vertexBuffer.getFloat(pos + positionOffset + 4) + position.y,
                    vertexBuffer.getFloat(pos + positionOffset + 8) + position.z,
                    1
            );


            //vec[a].add(Mth.sin(vec[a].x) * 0.2f, Mth.sin(vec[a].y)  * 0.2f, Mth.sin(vec[a].z) * 0.2f, 0);
            //vec[a].set(Math.atan(vec[a].x * 0.2) * 5, Math.atan(vec[a].y  * 0.2) * 5, Math.atan(vec[a].z  * 0.2) * 5, 1);
            workMat.transform(this.pos[a]);
        } else {
            this.pos[a].set(0);
        }

        if (colorOffset != -1) {
            var color = Integer.reverseBytes(vertexBuffer.getInt(pos + colorOffset));

            colors[a].set(
                    RGBA.redFloat(color),
                    RGBA.greenFloat(color),
                    RGBA.blueFloat(color),
                    RGBA.alphaFloat(color)
            ).mul(colorModulator);
        } else {
            colors[a].set(colorModulator);
        }

        if (uvOffset != -1) {
            uvs[a].set(vertexBuffer.getFloat(pos + uvOffset), vertexBuffer.getFloat(pos + uvOffset + 4));

            if (isGlint) {
                tmpVec4f.set(uvs[a], 0, 1);
                textureMat.transformProject(tmpVec4f);
                uvs[a].set(tmpVec4f.x, tmpVec4f.y);
            }
        } else if (triangleMode) {
            uvs[a].set(a == 2 ? 1 : 0, a == 0 ? 1 : 0);
        } else {
            uvs[a].set(a != 2 ? 1 : 0, a != 0 ? 1 : 0);
        }

        if (normalOffset != -1) {
            normals[a].set(
                    vertexBuffer.get(pos + normalOffset) / 128f,
                    vertexBuffer.get(pos + normalOffset + 1) / 128f,
                    vertexBuffer.get(pos + normalOffset + 2) / 128f
            );

            if (hasLighting) {
                var dotL1 = Math.max(lightDir0.dot(normals[a]), 0);
                var dotL2 = Math.max(lightDir1.dot(normals[a]), 0);

                float lightAccum = Math.min(1.0f, (dotL1 + dotL2) * 0.6f + 0.4f);
                colors[a].mul(lightAccum, lightAccum, lightAccum, 1);
            }
        } else {
            normals[a].set(0);
        }

        if (uv1Offset != -1) {
            var u = vertexBuffer.getShort(pos + uv1Offset);
            var v = vertexBuffer.getShort(pos + uv1Offset + 2);

            RGBA.toVector4f(sampler1.sampleRaw(u, v, 0, 0), tmpVec4f);
            var alpha = colors[a].w;
            colors[a].lerp(tmpVec4f, 1 - tmpVec4f.w);
            colors[a].w = alpha;
        }

        if (uv2Offset != -1) {
            var u = Mth.clamp(vertexBuffer.getShort(pos + uv2Offset) / 256f + 0.5f / 16f, 0.5f / 16f, 15.5f / 16f);
            var v = Mth.clamp(vertexBuffer.getShort(pos + uv2Offset + 2) / 256f + 0.5f / 16f, 0.5f / 16f, 15.5f / 16f);

            var color = sampler2.sample(0, 0, u, v, 0, 0);
            colors[a].mul(RGBA.redFloat(color), RGBA.greenFloat(color), RGBA.blueFloat(color), RGBA.alphaFloat(color));
        }

        if (this.lineWidthOffset != -1) {
            lineWidth[a] = vertexBuffer.getFloat(pos + this.lineWidthOffset);
        } else {
            lineWidth[a] = 1;
        }
    }

    private int clipTriangleWFast(int out) {
        final var clipSpace = 0.05f;
        var clip0 = pos[0].w < clipSpace ? 1 : 0;
        var clip1 = pos[1].w < clipSpace ? 1 : 0;
        var clip2 = pos[2].w < clipSpace ? 1 : 0;

        var outOfBounds = clip0 + clip1 + clip2;

        if (outOfBounds == 0) {
            copy(0, out++);
            copy(1, out++);
            copy(2, out++);
            return 3;
        } else if (outOfBounds == 1) {
            var clipIndex = (clip0 == 1 ? 0 : clip1 == 1 ? 1 : 2);

            var nextIndex = (clipIndex + 1) % 3;
            var previousIndex = (clipIndex - 1 + 3) % 3;

            var fracNext = (clipSpace - pos[clipIndex].w) / (pos[nextIndex].w - pos[clipIndex].w);
            var fracPrevious = (clipSpace - pos[clipIndex].w) / (pos[previousIndex].w - pos[clipIndex].w);

            lerp(clipIndex, previousIndex, out++, fracPrevious);
            lerp(clipIndex, nextIndex, out++, fracNext);
            copy(previousIndex, out++);

            lerp(clipIndex, nextIndex, out++, fracNext);
            copy(nextIndex, out++);
            copy(previousIndex, out++);

            return 6;
        } else if (outOfBounds == 2) {
            var notClippedIndex = (clip0 == 0 ? 0 : clip1 == 0 ? 1 : 2);
            var nextIndex = (notClippedIndex + 1) % 3;
            var previousIndex = (notClippedIndex - 1 + 3) % 3;
            var fracNext = (clipSpace - pos[notClippedIndex].w) / (pos[nextIndex].w - pos[notClippedIndex].w);
            var fracPrevious = (clipSpace - pos[notClippedIndex].w) / (pos[previousIndex].w - pos[notClippedIndex].w);

            lerp(notClippedIndex, previousIndex, out++, fracPrevious);
            copy(notClippedIndex, out++);
            lerp(notClippedIndex, nextIndex, out++, fracNext);

            return 3;
        }
        return 0;
    }

    private int clipLineWFast(int out) {
        final var clipSpace = 0.05f;
        var clip0 = pos[0].w < clipSpace ? 1 : 0;
        var clip1 = pos[1].w < clipSpace ? 1 : 0;

        var outOfBounds = clip0 + clip1;

        if (outOfBounds == 0) {
            copy(0, out++);
            copy(1, out++);
            return 2;
        } else if (outOfBounds == 1) {
            var clipIndex = clip0 == 1 ? 0 : 1;
            var nextIndex = (clipIndex + 1) % 2;

            var fracNext = (clipSpace - pos[clipIndex].w) / (pos[nextIndex].w - pos[clipIndex].w);

            lerp(clipIndex, nextIndex, out++, fracNext);
            copy(nextIndex, out++);
            return 4;
        }
        return 0;
    }

    private float signedTriangleArea(float ax, float ay, float bx, float by, float cx, float cy) {
        return Math.fma(by - ay, bx + ax, Math.fma(cy - by, cx + bx, (ay - cy) * (ax + cx)));
    }

    private void drawTriangle(RGBATexture color, @Nullable DepthTexture depth, Scissor scissor,
                              int offset, Vector4f[] vec, Vector2f[] uvs, Vector4f[] colors,
                              @Nullable SoftShader texture, int layer) {
        var vec0 = vec[offset];
        var vec1 = vec[offset + 1];
        var vec2 = vec[offset + 2];

        var minX = (int) Math.min(vec0.x, Math.min(vec1.x, vec2.x));
        var maxX = (int) Math.max(vec0.x, Math.max(vec1.x, vec2.x));
        var minY = (int) Math.min(vec0.y, Math.min(vec1.y, vec2.y));
        var maxY = (int) Math.max(vec0.y, Math.max(vec1.y, vec2.y));

        if (minX > scissor.x2() || maxX < scissor.x1() || minY > scissor.y2() || maxY < scissor.y1()) {
            return;
        }

        minX = Math.max(minX, scissor.x1);
        maxX = Math.min(maxX, scissor.x2 - 1);
        minY = Math.max(minY, scissor.y1);
        maxY = Math.min(maxY, scissor.y2 - 1);

        var drawnColor = new Vector4f();
        var originalColor = new Vector4f();

        var uv0 = uvs[offset];
        var uv1 = uvs[offset + 1];
        var uv2 = uvs[offset + 2];

        var color0 = colors[offset];
        var color1 = colors[offset + 1];
        var color2 = colors[offset + 2];

        var sameColor = color0.equals(color1) && color0.equals(color2);
        var noColor = sameColor && color0.equals(1, 1, 1, 1);
        var calculateZ = vec0.z != vec1.z || vec0.z != vec2.z;

        var totalArea = signedTriangleArea(vec0.x, vec0.y, vec1.x, vec1.y, vec2.x, vec2.y);
        if (totalArea < 0.05f && (this.pipeline.isCull() || totalArea > -0.05f)) return;
        var side = Mth.sign(totalArea);

        totalArea = 1 / totalArea;
        //for (int y = minY & 0xFFFFFFF8; y <= maxY; y += 8) {

        for (int y = minY; y <= maxY; y++) {
            int yOffset = color.width() * y;
            for (int x = minX; x <= maxX; x++) {

                var alpha = signedTriangleArea(x + 0.4995f, y + 0.4995f, vec1.x, vec1.y, vec2.x, vec2.y);
                if (alpha * side < 0) continue;

                var beta = signedTriangleArea(x + 0.4995f, y + 0.4995f, vec2.x, vec2.y, vec0.x, vec0.y);
                if (beta * side < 0) continue;

                var gamma = signedTriangleArea(x + 0.4995f, y + 0.4995f, vec0.x, vec0.y, vec1.x, vec1.y);
                if (gamma * side < 0) continue;

                alpha *= totalArea;
                beta *= totalArea;
                gamma *= totalArea;

                var z = calculateZ ? Math.fma(alpha, vec0.z, Math.fma(beta, vec1.z, gamma * vec2.z)) : vec0.z;

                if (depth != null) {
                    z = this.applyDepthBias(z, depth.data()[x + yOffset]);
                }

                if (this.limitDepth && (z < CLIP_Z_MIN - 0.00001f || z > 1.00001f)) {
                    continue;
                }

                if (depth == null || this.depthTest.test(depth.data()[x + yOffset], z)) {
                    alpha *= vec0.w;
                    beta *= vec1.w;
                    gamma *= vec2.w;
                    var invW = 1 / (alpha + beta + gamma);

                    if (texture != null) {
                        var u = Math.fma(alpha, uv0.x, Math.fma(beta, uv1.x, gamma * uv2.x)) * invW;
                        var v = Math.fma(alpha, uv0.y, Math.fma(beta, uv1.y, gamma * uv2.y)) * invW;
                        RGBA.toVector4f(texture.sample(x, y, u, v, layer, 0), drawnColor);
                    } else {
                        drawnColor.set(1f);
                    }

                    if (noColor) {
                    } else if (sameColor) {
                        drawnColor.mul(color0);
                    } else {
                        drawnColor.mul(
                                Math.fma(color0.x, alpha, Math.fma(color1.x, beta, color2.x * gamma)) * invW,
                                Math.fma(color0.y, alpha, Math.fma(color1.y, beta, color2.y * gamma)) * invW,
                                Math.fma(color0.z, alpha, Math.fma(color1.z, beta, color2.z * gamma)) * invW,
                                Math.fma(color0.w, alpha, Math.fma(color1.w, beta, color2.w * gamma)) * invW
                        );
                    }

                    if (this.blender == ColorBlender.SOLID) {
                        drawnColor.w = 1;
                    } else if (drawnColor.w < 0.05) {
                        continue;
                    }

                    if (this.writeDepth && depth != null) {
                        depth.data()[x + yOffset] = z;
                    }

                    var out = this.blender.blend(RGBA.toVector4f(color.data()[x + yOffset], originalColor), drawnColor);
                    color.data()[x + yOffset] = out;
                }
            }
        }
    }

    private float applyDepthBias(float z, float sourceZ) {
        return z + (z - sourceZ) * this.depthBiasScaleFactor + this.depthBiasConstant;
    }

    private void drawLine(RGBATexture color, @Nullable DepthTexture depth, Scissor scissor,
                          int offset, Vector4f[] vec, Vector2f[] uvs, Vector4f[] colors,
                          float[] lineWidth, @Nullable SoftShader texture, int layer) {
        var vec0 = vec[offset];
        var vec1 = vec[offset + 1];

        var minX = (int) Math.min(vec0.x, vec1.x);
        var maxX = (int) Math.max(vec0.x, vec1.x);
        var minY = (int) Math.min(vec0.y, vec1.y);
        var maxY = (int) Math.max(vec0.y, vec1.y);

        if (minX > scissor.x2() || maxX < scissor.x1() || minY > scissor.y2() || maxY < scissor.y1()) {
            return;
        }

        var lineWidth0 = lineWidth[offset] * 0.5f;
        var lineWidth1 = lineWidth[offset + 1];

        var boundaryMargin = (int) Math.max(lineWidth0, lineWidth1);

        minX = Math.max(minX - boundaryMargin, scissor.x1);
        maxX = Math.min(maxX + boundaryMargin, scissor.x2 - 1);
        minY = Math.max(minY - boundaryMargin, scissor.y1);
        maxY = Math.min(maxY + boundaryMargin, scissor.y2 - 1);

        var drawnColor = new Vector4f();
        var originalColor = new Vector4f();

        var uv0 = uvs[offset];
        var uv1 = uvs[offset + 1];

        var color0 = colors[offset];
        var color1 = colors[offset + 1];

        var length = vec0.distance(vec1.x, vec1.y, vec0.z, vec0.w);
        var lengthWidth = Mth.square(length + lineWidth0);

        for (int y = minY; y <= maxY; y++) {
            int yOffset = color.width() * y;
            for (int x = minX; x <= maxX; x++) {
                var r = Math.abs(signedTriangleArea(x + 0.5f, y + 0.5f, vec0.x, vec0.y, vec1.x, vec1.y)) / length;

                if (r > lineWidth0) continue;
                var distFrom0 = vec0.distanceSquared(x + 0.5f, y + 0.5f, vec0.z, vec0.w);
                var distFrom1 = vec1.distanceSquared(x + 0.5f, y + 0.5f, vec1.z, vec1.w);

                if (distFrom0 > lengthWidth || distFrom1 > lengthWidth) continue;

                var dist = Mth.sqrt(distFrom0 - r * r);

                var delta = Mth.clamp(dist / length, 0, 1);

                var z = Mth.lerp(delta, vec0.z, vec1.z);

                if (depth != null) {
                    z = this.applyDepthBias(z, depth.data()[x + yOffset]);
                }

                if (this.limitDepth && (z < CLIP_Z_MIN || z > 1)) {
                    continue;
                }

                if (depth == null || this.depthTest.test(depth.data()[x + yOffset], z)) {
                    if (texture != null) {
                        var u = Mth.lerp(delta, uv0.x, uv1.x);
                        var v = Mth.lerp(r, uv0.y, uv1.y);
                        RGBA.toVector4f(texture.sample(x, y, u, v, layer, 0), drawnColor);
                    } else {
                        drawnColor.set(1f);
                    }

                    drawnColor.mul(
                            Mth.lerp(delta, color0.x, color1.x),
                            Mth.lerp(delta, color0.y, color1.y),
                            Mth.lerp(delta, color0.z, color1.z),
                            Mth.lerp(delta, color0.w, color1.w)
                    );

                    if (this.blender == ColorBlender.SOLID) {
                        drawnColor.w = 1;
                    } else if (drawnColor.w < 0.05) {
                        continue;
                    }

                    if (this.writeDepth && depth != null) {
                        depth.data()[x + yOffset] = z;
                    }

                    var out = this.blender.blend(RGBA.toVector4f(color.data()[x + yOffset], originalColor), drawnColor);
                    color.data()[x + yOffset] = out;
                }
            }
        }
    }



    private interface DepthTestPredicate {
        boolean test(float image, float drawn);
    }

    public interface DrawCall {
        void draw(ByteBuffer vertexBuffer, @Nullable ByteBuffer indexBuffer, int baseVertex, int firstIndex, int drawCount, VertexFormat.@Nullable IndexType indexType, int instanceCount, Map<String, Std140Reader> uniforms);
    }

    private record Scissor(int x1, int y1, int x2, int y2) {
        public boolean inBounds(int x, int y) {
            return x >= x1 - 1 && x < x2 + 1 && y >= y1 - 1 && y < y2 + 1;
        }

        public boolean inBounds(float x, float y) {
            return x >= x1 - 1 && x < x2 + 1 && y >= y1 - 1 && y < y2 + 1;
        }

        public int inBoundsInt(float x, float y) {
            return x >= x1 - 1 && x < x2 + 1 && y >= y1 - 1 && y < y2 + 1 ? 1 : 0;
        }
    }

    private record ColorBlender(String name, ToIntBiFunction<Vector4f, Vector4f> blend) {
        public static final ColorBlender SOLID = new ColorBlender("solid", (image, drawn) -> RGBA.fromVector4f(drawn) | 0xFF);
        public static final ColorBlender CUTOUT = new ColorBlender("cutout", (image, drawn) -> RGBA.fromVector4f(drawn.w > 0.1f ? drawn : image));
        public static final ColorBlender PREMULTIPLIED_BLEND = new ColorBlender("premultiplied_blend", (image, drawn) -> RGBA.fromVector4f(image.lerp(drawn, drawn.w)) | 0xFF);
        public static final ColorBlender ALPHA_BLEND = new ColorBlender("alpha_blend", (image, drawn) -> RGBA.alphaBlend(RGBA.fromVector4f(image), RGBA.fromVector4f(drawn)));
        public static final ColorBlender INVERT = new ColorBlender("invert", (image, drawn) -> RGBA.fromVector4f(image.mul(-1, -1, -1, 1).add(1, 1, 1, 0)));
        public static final ColorBlender ADDITIVE = new ColorBlender("additive", (image, drawn) -> RGBA.colorFromFloatRGBA(
                Math.clamp(image.x + drawn.x, 0, 1),
                Math.clamp(image.y + drawn.y, 0, 1),
                Math.clamp(image.z + drawn.z, 0, 1),
                Math.clamp(image.w + drawn.w, 0, 1)
        ));
        public static final ColorBlender SUBTRACTIVE = new ColorBlender("subtractive", (image, drawn) -> RGBA.colorFromFloatRGBA(
                Math.clamp(image.x - drawn.x, 0, 1),
                Math.clamp(image.y - drawn.y, 0, 1),
                Math.clamp(image.z - drawn.z, 0, 1),
                image.w
        ));

        public static final ColorBlender MULTIPLY = new ColorBlender("multiply", (image, drawn) -> RGBA.fromVector4f(image.mul(drawn.x, drawn.y, drawn.z, 1)));
        public static final ColorBlender OVERLAY = new ColorBlender("overlay", (image, drawn) -> RGBA.colorFromFloatRGBA(
                Math.clamp(image.x + drawn.x * drawn.w, 0, 1),
                Math.clamp(image.y + drawn.y * drawn.w, 0, 1),
                Math.clamp(image.z + drawn.z * drawn.w, 0, 1),
                image.w
        ));

        int blend(Vector4f image, Vector4f drawn) {
            return this.blend.applyAsInt(image, drawn);
        }
    }
}
