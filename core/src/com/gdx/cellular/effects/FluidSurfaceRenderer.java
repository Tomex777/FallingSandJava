package com.gdx.cellular.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.liquid.Water;

/**
 * A presentation layer, independent of the cellular physics grid.
 * A tiny one-texel-per-cell water mask is uploaded once per rendered frame.
 * The GPU smoothly contours the fluid and adds cheap animated glints.
 * Simulation, collisions, heat and save/load are unchanged.
 */
public final class FluidSurfaceRenderer implements Disposable {
    private static final String VERTEX =
            "attribute vec4 a_position;\n"
            + "attribute vec4 a_color;\n"
            + "attribute vec2 a_texCoord0;\n"
            + "uniform mat4 u_projTrans;\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "void main(){ v_color = a_color; v_color.a *= (255.0/254.0);"
            + "v_texCoords = a_texCoord0; gl_Position = u_projTrans * a_position; }\n";

    private static final String FRAGMENT =
            "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoords;\n"
            + "uniform sampler2D u_texture;\n"
            + "uniform vec2 u_texel;\n"
            + "uniform float u_time;\n"
            + "uniform float u_detail;\n"
            + "float mask(vec2 uv){ return texture2D(u_texture, uv).r; }\n"
            + "void main(){\n"
            + "  vec2 uv = v_texCoords;\n"
            + "  float c = mask(uv);\n"
            + "  if(c < 0.045) discard;\n"
            + "  float l = mask(uv - vec2(u_texel.x,0.0));\n"
            + "  float r = mask(uv + vec2(u_texel.x,0.0));\n"
            + "  float up = mask(uv - vec2(0.0,u_texel.y));\n"
            + "  float down = u_detail > 0.5 ? mask(uv + vec2(0.0,u_texel.y)) : c;\n"
            + "  float smoothMask = (c*4.0+l+r+up+down)/8.0;\n"
            + "  float coverage = smoothstep(0.20,0.72,smoothMask);\n"
            + "  if(coverage < 0.012) discard;\n"
            + "  float top = max(0.0,c-up);\n"
            + "  float wave = u_detail > 0.5 ? sin(uv.x*103.0 + u_time*2.1 + uv.y*21.0)*0.032 : 0.0;\n"
            + "  float shine = (top * 0.22) + (r-l)*0.055 + wave;\n"
            + "  vec3 base = vec3(0.08,0.39,0.70);\n"
            + "  vec3 lit = clamp(base + vec3(shine*0.75, shine, shine*1.12),0.0,1.0);\n"
            + "  gl_FragColor = vec4(lit*v_color.rgb, coverage*0.91*v_color.a);\n"
            + "}\n";

    private static final int PATCH_SIZE = 32;
    private static final int MAX_PATCH_UPLOADS = 3;
    private static final int WATER_PIXEL = 0xffffffff;
    private static final int EMPTY_PIXEL = 0x000000ff;

    private Pixmap maskPixmap;
    // Pause, editing and loading can change pixels without simulating a step.
    private byte[] previousWater;
    private final Pixmap[] uploadPatches = new Pixmap[4];
    private long scanNanosLastFrame;
    private long uploadNanosLastFrame;
    private int textureUploadsLastFrame;
    private int changedCellsLastFrame;
    private boolean lowDetail;
    private int slowFrames;
    private Texture maskTexture;
    private SpriteBatch batch;
    private ShaderProgram shader;
    private int width;
    private int height;
    private float time;
    private boolean available;
    private long lastMetricsNanos;

    public FluidSurfaceRenderer(CellularMatrix matrix) {
        try {
            width = matrix.innerArraySize;
            height = matrix.outerArraySize;
            maskPixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            maskPixmap.setColor(Color.BLACK);
            maskPixmap.fill();
            previousWater = new byte[width * height];
            maskTexture = new Texture(maskPixmap);
            maskTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            ShaderProgram.pedantic = false;
            shader = new ShaderProgram(VERTEX, FRAGMENT);
            if (!shader.isCompiled()) {
                Gdx.app.error("ElementumFluid", "Shader fallback: " + shader.getLog());
                return;
            }
            // A single bounding quad is drawn per frame. Avoid allocating a
            // 500-sprite vertex buffer on memory-constrained Android devices.
            batch = new SpriteBatch(1, shader);
            available = true;
            String gpuName = Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_RENDERER);
            if (gpuName != null) {
                String lower = gpuName.toLowerCase(java.util.Locale.ROOT);
                lowDetail = lower.contains("swiftshader") || lower.contains("llvmpipe")
                        || lower.contains("mali-400") || lower.contains("adreno (tm) 3");
            }
            Gdx.app.log("ElementumFluid", "smooth-water-shader=enabled gpu="
                    + gpuName + " detail=" + (lowDetail ? "low" : "full"));
        } catch (Throwable failure) {
            Gdx.app.error("ElementumFluid", "Liquid GPU pass unavailable", failure);
        }
    }

    /** Used by the cellular renderer to leave water entirely to the smooth pass. */
    public boolean isAvailable() {
        return available;
    }

    public void render(CellularMatrix matrix, OrthographicCamera camera, boolean paused) {
        if (!available) return;
        if (matrix.innerArraySize != width || matrix.outerArraySize != height) return;
        try {
            if (!paused) time += Math.min(0.05f, Gdx.graphics.getDeltaTime());
            // Only update texels that changed. In particular a paused pool
            // needs no repeated full-mask upload.
            long scanStart = System.nanoTime();
            int occupied = 0;
            int minX = width, minY = height, maxX = -1, maxY = -1;
            int dirtyMinX = width, dirtyMinY = height;
            int dirtyMaxX = -1, dirtyMaxY = -1;
            int changed = 0;
            for (int y = 0; y < height; y++) {
                Array<Element> row = matrix.getRow(y);
                int rowStart = y * width;
                for (int x = 0; x < width; x++) {
                    Element element = row.get(x);
                    boolean water = element instanceof Water && element.owningBody == null;
                    int index = rowStart + x;
                    byte value = (byte) (water ? 1 : 0);
                    if (previousWater[index] != value) {
                        previousWater[index] = value;
                        maskPixmap.drawPixel(x, height - 1 - y,
                                water ? WATER_PIXEL : EMPTY_PIXEL);
                        changed++;
                        if (x < dirtyMinX) dirtyMinX = x;
                        if (x > dirtyMaxX) dirtyMaxX = x;
                        if (y < dirtyMinY) dirtyMinY = y;
                        if (y > dirtyMaxY) dirtyMaxY = y;
                    }
                    if (water) {
                        occupied++;
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                    }
                }
            }
            scanNanosLastFrame = System.nanoTime() - scanStart;
            changedCellsLastFrame = changed;
            textureUploadsLastFrame = 0;
            uploadNanosLastFrame = 0L;
            // Clearing the last cell MUST reach the texture to avoid stale
            // islands resurfacing when a later pool overlaps the old area.
            if (changed > 0) {
                long uploadStart = System.nanoTime();
                textureUploadsLastFrame = uploadChanges(
                        dirtyMinX, dirtyMinY, dirtyMaxX, dirtyMaxY);
                uploadNanosLastFrame = System.nanoTime() - uploadStart;
            }
            if (occupied == 0) return;

            minX = Math.max(0, minX - 2);
            minY = Math.max(0, minY - 2);
            maxX = Math.min(width - 1, maxX + 2);
            maxY = Math.min(height - 1, maxY + 2);
            int cellsWide = maxX - minX + 1;
            int cellsHigh = maxY - minY + 1;
            float scale = matrix.pixelSizeModifier;

            // One-way adaptive cheaper detail on sustained slow frame pacing;
            // no duplicate quality slider, and physics are untouched.
            if (!lowDetail && !paused) {
                float delta = Gdx.graphics.getDeltaTime();
                slowFrames = delta >= 0.038f ? Math.min(180, slowFrames + 1)
                        : Math.max(0, slowFrames - 2);
                if (slowFrames >= 120) {
                    lowDetail = true;
                    Gdx.app.log("ElementumFluid", "detail=low sustained-slow-frames");
                }
            }

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            shader.setUniformf("u_texel", 1f / width, 1f / height);
            shader.setUniformf("u_time", time);
            shader.setUniformf("u_detail", lowDetail ? 0f : 1f);
            // SpriteBatch draws the bottom-left vertex with v and the
            // top-left with v2; matrix Y=0 maps to bottom Pixmap row.
            // Match the exact outer texel edges (no half-cell shift).
            batch.draw(maskTexture, minX * scale, minY * scale,
                    cellsWide * scale, cellsHigh * scale,
                    (float) minX / width, (float) (height - minY) / height,
                    (float) (maxX + 1) / width, (float) (height - 1 - maxY) / height);
            batch.end();

            long now = System.nanoTime();
            if (now - lastMetricsNanos >= 5_000_000_000L) {
                long total = (long) width * height;
                Gdx.app.log("ElementumFluid", "water-cells=" + occupied
                        + " changed-cells=" + changedCellsLastFrame
                        + " mask-uploads=" + textureUploadsLastFrame
                        + " mask-scan-us=" + (scanNanosLastFrame / 1000L)
                        + " mask-upload-us=" + (uploadNanosLastFrame / 1000L)
                        + " shaded-grid-cells=" + (cellsWide * cellsHigh)
                        + " grid-cells=" + total
                        + " shader-region-percent="
                        + (100L * cellsWide * cellsHigh / Math.max(1L, total))
                        + " detail=" + (lowDetail ? "low" : "full")
                        + " fps=" + Gdx.graphics.getFramesPerSecond());
                lastMetricsNanos = now;
            }
        } catch (Throwable failure) {
            available = false;
            Gdx.app.error("ElementumFluid", "Smooth liquid pass disabled after error", failure);
            // The original cellular water drawer automatically resumes
            // on the next frame when isAvailable() becomes false.
        }
    }

    /**
     * Upload up to three changed tiles with reusable 32x32 Pixmaps, or a
     * single whole-mask upload if the changed region is too scattered.
     * No per-frame temporary Pixmaps, offscreen passes or GPU readbacks.
     */
    private int uploadChanges(int minX, int minY, int maxX, int maxY) {
        int firstCol = minX / PATCH_SIZE;
        int lastCol = maxX / PATCH_SIZE;
        int firstRow = (height - 1 - maxY) / PATCH_SIZE;
        int lastRow = (height - 1 - minY) / PATCH_SIZE;
        int tileCount = (lastCol - firstCol + 1) * (lastRow - firstRow + 1);
        long uploadedPixels = (long) tileCount * PATCH_SIZE * PATCH_SIZE;
        if (tileCount > MAX_PATCH_UPLOADS
                || uploadedPixels >= (long) width * height / 2L) {
            maskTexture.draw(maskPixmap, 0, 0);
            return 1;
        }
        for (int row = firstRow; row <= lastRow; row++) {
            int tileY = row * PATCH_SIZE;
            int patchHeight = Math.min(PATCH_SIZE, height - tileY);
            for (int col = firstCol; col <= lastCol; col++) {
                int tileX = col * PATCH_SIZE;
                int patchWidth = Math.min(PATCH_SIZE, width - tileX);
                Pixmap tile = patch(patchWidth, patchHeight);
                tile.drawPixmap(maskPixmap, tileX, tileY,
                        patchWidth, patchHeight, 0, 0);
                maskTexture.draw(tile, tileX, tileY);
            }
        }
        return tileCount;
    }

    private Pixmap patch(int patchWidth, int patchHeight) {
        int index = (patchWidth == PATCH_SIZE ? 0 : 1)
                + (patchHeight == PATCH_SIZE ? 0 : 2);
        Pixmap result = uploadPatches[index];
        if (result == null) {
            result = new Pixmap(patchWidth, patchHeight, Pixmap.Format.RGBA8888);
            result.setBlending(Pixmap.Blending.None);
            uploadPatches[index] = result;
        }
        return result;
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shader != null) shader.dispose();
        if (maskTexture != null) maskTexture.dispose();
        if (maskPixmap != null) maskPixmap.dispose();
        for (Pixmap tile : uploadPatches) {
            if (tile != null) tile.dispose();
        }
    }
}
