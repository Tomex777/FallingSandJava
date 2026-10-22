package com.gdx.cellular.effects;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.PixmapTextureData;
import com.badlogic.gdx.graphics.GL20;
import java.util.Arrays;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.gdx.cellular.CellularMatrix;
import com.gdx.cellular.effects.fluid.FluidMaskGeometry;
import com.gdx.cellular.elements.Element;
import com.gdx.cellular.elements.liquid.Water;

/**
 * A presentation layer, independent of the cellular physics grid.
 * A tiny one-texel-per-cell water mask uploads only when its contents change.
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
            // Animate highlights only on exposed top edges: a motionless
            // deep pool should not shimmer across its whole interior.
            + "  float shine = (top * 0.22) + (r-l)*0.055 + wave*top;\n"
            + "  vec3 base = vec3(0.08,0.39,0.70);\n"
            + "  vec3 lit = clamp(base + vec3(shine*0.75, shine, shine*1.12),0.0,1.0);\n"
            + "  gl_FragColor = vec4(lit*v_color.rgb, coverage*0.91*v_color.a);\n"
            + "}\n";

    // sin(u_time * 2.1) repeats after four complete ripples. Keeping the
    // uniform below ~12 seconds avoids mediump-float loss of animation
    // precision on older mobile fragment shaders during long sessions.
    private static final float RIPPLE_LOOP_SECONDS =
            (float) (Math.PI * 2.0 * 4.0 / 2.1);
    private static final int PATCH_SIZE = 32;
    private static final int MAX_PATCH_UPLOADS = 3;
    private static final int WATER_PIXEL = 0xffffffff;
    private static final int EMPTY_PIXEL = 0x000000ff;

    private Pixmap maskPixmap;
    // Pause, editing and loading can change pixels without simulating a step.
    private byte[] previousWater;
    private final Pixmap[] uploadPatches = new Pixmap[4];
    // Sparse tile flags avoid reuploading a whole bounding rectangle when
    // two distant droplets change in different corners of the mask.
    private boolean[] dirtyTiles;
    private int tileColumns;
    private long scanNanosLastFrame;
    private long uploadNanosLastFrame;
    private int textureUploadsLastFrame;
    private int uploadedPixelsLastFrame;
    private long scanWindowNanos;
    private long uploadWindowNanos;
    private long maxScanWindowNanos;
    private long maxUploadWindowNanos;
    private long changedWindowCells;
    private long uploadedWindowPixels;
    private int textureWindowUpdates;
    private int metricWindowFrames;
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
            tileColumns = (width + PATCH_SIZE - 1) / PATCH_SIZE;
            int tileRows = (height + PATCH_SIZE - 1) / PATCH_SIZE;
            dirtyTiles = new boolean[tileColumns * tileRows];
            // A persistently backed managed mask restores itself when Android
            // recreates its GL context, without unsafe stale-handle tests.
            // We retain the Pixmap as the current CPU-side water occupancy.
            maskTexture = new Texture(new PixmapTextureData(
                    maskPixmap, Pixmap.Format.RGBA8888, false, false, true));
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
            String gpuName = Gdx.gl.glGetString(com.badlogic.gdx.graphics.GL20.GL_RENDERER);
            if (gpuName != null) {
                String lower = gpuName.toLowerCase(java.util.Locale.ROOT);
                lowDetail = lower.contains("swiftshader") || lower.contains("llvmpipe")
                        || lower.contains("mali-400") || lower.contains("adreno (tm) 3");
            }
            available = true;
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
            if (!paused) {
                time += Math.min(0.05f, Gdx.graphics.getDeltaTime());
                if (time >= RIPPLE_LOOP_SECONDS) time -= RIPPLE_LOOP_SECONDS;
            }
            // Only update texels that changed. In particular a paused pool
            // needs no repeated full-mask upload.
            long scanStart = System.nanoTime();
            int occupied = 0;
            int minX = width, minY = height, maxX = -1, maxY = -1;
            Arrays.fill(dirtyTiles, false);
            int dirtyTileCount = 0;
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
                        maskPixmap.drawPixel(x, FluidMaskGeometry.pixmapY(y, height),
                                water ? WATER_PIXEL : EMPTY_PIXEL);
                        changed++;
                        int dirtyTileIndex = FluidMaskGeometry.tileIndex(
                                x, y, width, height, PATCH_SIZE);
                        if (!dirtyTiles[dirtyTileIndex]) {
                            dirtyTiles[dirtyTileIndex] = true;
                            dirtyTileCount++;
                        }
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
            uploadedPixelsLastFrame = 0;
            uploadNanosLastFrame = 0L;
            // Clearing the last cell MUST reach the texture to avoid stale
            // islands resurfacing when a later pool overlaps the old area.
            if (changed > 0) {
                long uploadStart = System.nanoTime();
                textureUploadsLastFrame = uploadChanges(dirtyTileCount);
                uploadNanosLastFrame = System.nanoTime() - uploadStart;
            }
            if (occupied == 0) {
                logWaterMetrics(0, 0);
                return;
            }

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
                    (float) minX / width, FluidMaskGeometry.bottomV(minY, height),
                    (float) (maxX + 1) / width, FluidMaskGeometry.topV(maxY, height));
            batch.end();

            logWaterMetrics(occupied, cellsWide * cellsHigh);
        } catch (Throwable failure) {
            available = false;
            Gdx.app.error("ElementumFluid", "Smooth liquid pass disabled after error", failure);
            // The original cellular water drawer automatically resumes
            // on the next frame when isAvailable() becomes false.
        }
    }

    /**
     * Upload actual dirty tiles (not the enclosing rectangle) when sparse.
     * Three distant droplets can use three bounded updates even if their
     * combined bounding rectangle is almost the entire world.
     */
    private int uploadChanges(int dirtyCount) {
        if (FluidMaskGeometry.useFullUpload(dirtyCount, PATCH_SIZE,
                width, height, MAX_PATCH_UPLOADS)) {
            uploadMask(maskPixmap, 0, 0);
            uploadedPixelsLastFrame = width * height;
            return 1;
        }

        int pixels = 0;
        for (int index = 0; index < dirtyTiles.length; index++) {
            if (!dirtyTiles[index]) continue;
            int tileX = (index % tileColumns) * PATCH_SIZE;
            int tileY = (index / tileColumns) * PATCH_SIZE;
            int patchWidth = Math.min(PATCH_SIZE, width - tileX);
            int patchHeight = Math.min(PATCH_SIZE, height - tileY);
            Pixmap tile = patch(patchWidth, patchHeight);
            tile.drawPixmap(maskPixmap, 0, 0,
                    tileX, tileY, patchWidth, patchHeight);
            uploadMask(tile, tileX, tileY);
            pixels += patchWidth * patchHeight;
        }
        uploadedPixelsLastFrame = pixels;
        return dirtyCount;
    }

    /**
     * libGDX Texture.draw rejects managed textures even though a managed
     * PixmapTextureData can safely retain its backing Pixmap across EGL loss.
     * This is the same bounded glTexSubImage2D upload that Texture.draw uses.
     * libGDX automatically reloads the full up-to-date mask from the Pixmap
     * after Android reconstructs the GL context.
     */
    private void uploadMask(Pixmap pixels, int x, int y) {
        maskTexture.bind();
        Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, x, y,
                pixels.getWidth(), pixels.getHeight(),
                pixels.getGLFormat(), pixels.getGLType(), pixels.getPixels());
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

    /**
     * Fixed five-second windows are more useful than a random last-frame
     * sample: track actual mask scanning cost, uploads and avoided bandwidth.
     * Renderer time is CPU-side wall time, not an unverified GPU timer.
     */
    private void logWaterMetrics(int occupied, int shadedCells) {
        metricWindowFrames++;
        scanWindowNanos += scanNanosLastFrame;
        uploadWindowNanos += uploadNanosLastFrame;
        maxScanWindowNanos = Math.max(maxScanWindowNanos, scanNanosLastFrame);
        maxUploadWindowNanos = Math.max(maxUploadWindowNanos, uploadNanosLastFrame);
        changedWindowCells += changedCellsLastFrame;
        textureWindowUpdates += textureUploadsLastFrame;
        uploadedWindowPixels += uploadedPixelsLastFrame;

        long now = System.nanoTime();
        if (lastMetricsNanos == 0L) {
            lastMetricsNanos = now;
            return;
        }
        if (now - lastMetricsNanos < 5_000_000_000L) return;

        long total = (long) width * height;
        Gdx.app.log("ElementumFluid", "frames=" + metricWindowFrames
                + " water-cells=" + occupied
                + " changed-cells-total=" + changedWindowCells
                + " mask-uploads-total=" + textureWindowUpdates
                + " uploaded-kib=" + (uploadedWindowPixels * 4L / 1024L)
                + " avg-mask-scan-us=" + (scanWindowNanos / Math.max(1, metricWindowFrames) / 1000L)
                + " max-mask-scan-us=" + (maxScanWindowNanos / 1000L)
                + " avg-mask-upload-us=" + (uploadWindowNanos / Math.max(1, metricWindowFrames) / 1000L)
                + " max-mask-upload-us=" + (maxUploadWindowNanos / 1000L)
                + " shaded-grid-cells=" + shadedCells
                + " grid-cells=" + total
                + " shader-region-percent=" + (100L * shadedCells / Math.max(1L, total))
                + " detail=" + (lowDetail ? "low" : "full")
                + " fps=" + Gdx.graphics.getFramesPerSecond());

        metricWindowFrames = 0;
        scanWindowNanos = 0L;
        uploadWindowNanos = 0L;
        maxScanWindowNanos = 0L;
        maxUploadWindowNanos = 0L;
        changedWindowCells = 0L;
        uploadedWindowPixels = 0L;
        textureWindowUpdates = 0;
        lastMetricsNanos = now;
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
