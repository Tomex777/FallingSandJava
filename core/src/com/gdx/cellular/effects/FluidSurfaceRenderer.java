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
            + "float mask(vec2 uv){ return texture2D(u_texture, uv).r; }\n"
            + "void main(){\n"
            + "  vec2 uv = v_texCoords;\n"
            + "  float c = mask(uv);\n"
            + "  float l = mask(uv - vec2(u_texel.x,0.0));\n"
            + "  float r = mask(uv + vec2(u_texel.x,0.0));\n"
            + "  float up = mask(uv - vec2(0.0,u_texel.y));\n"
            + "  float down = mask(uv + vec2(0.0,u_texel.y));\n"
            + "  float smoothMask = (c*4.0+l+r+up+down)/8.0;\n"
            + "  float coverage = smoothstep(0.20,0.72,smoothMask);\n"
            + "  if(coverage < 0.012) discard;\n"
            + "  float top = max(0.0,c-up);\n"
            + "  float wave = sin(uv.x*103.0 + u_time*2.1 + uv.y*21.0)*0.032;\n"
            + "  float shine = (top * 0.22) + (r-l)*0.055 + wave;\n"
            + "  vec3 base = vec3(0.08,0.39,0.70);\n"
            + "  vec3 lit = clamp(base + vec3(shine*0.75, shine, shine*1.12),0.0,1.0);\n"
            + "  gl_FragColor = vec4(lit*v_color.rgb, coverage*0.91*v_color.a);\n"
            + "}\n";

    private Pixmap maskPixmap;
    private Texture maskTexture;
    private SpriteBatch batch;
    private ShaderProgram shader;
    private int width;
    private int height;
    private float time;
    private boolean available;

    public FluidSurfaceRenderer(CellularMatrix matrix) {
        try {
            width = matrix.innerArraySize;
            height = matrix.outerArraySize;
            maskPixmap = new Pixmap(width, height, Pixmap.Format.RGBA8888);
            maskPixmap.setColor(Color.BLACK);
            maskPixmap.fill();
            maskTexture = new Texture(maskPixmap);
            maskTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            ShaderProgram.pedantic = false;
            shader = new ShaderProgram(VERTEX, FRAGMENT);
            if (!shader.isCompiled()) {
                Gdx.app.error("ElementumFluid", "Shader fallback: " + shader.getLog());
                return;
            }
            batch = new SpriteBatch(500, shader);
            available = true;
            Gdx.app.log("ElementumFluid", "smooth-water-shader=enabled");
        } catch (Throwable failure) {
            Gdx.app.error("ElementumFluid", "Liquid GPU pass unavailable", failure);
        }
    }

    public void render(CellularMatrix matrix, OrthographicCamera camera, boolean paused) {
        if (!available) return;
        if (matrix.innerArraySize != width || matrix.outerArraySize != height) return;
        try {
            if (!paused) time += Math.min(0.05f, Gdx.graphics.getDeltaTime());
            maskPixmap.setColor(Color.BLACK);
            maskPixmap.fill();
            maskPixmap.setColor(Color.WHITE);
            int occupied = 0;
            for (int y = 0; y < height; y++) {
                Array<Element> row = matrix.getRow(y);
                for (int x = 0; x < width; x++) {
                    Element e = row.get(x);
                    if (e instanceof Water && e.owningBody == null) {
                        // Pixmap rows originate at the top. Mirror the matrix's
                        // bottom-origin Y before uploading to the GPU.
                        maskPixmap.drawPixel(x, height - 1 - y);
                        occupied++;
                    }
                }
            }
            if (occupied == 0) return;
            maskTexture.draw(maskPixmap, 0, 0);
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            shader.setUniformf("u_texel", 1f/width, 1f/height);
            shader.setUniformf("u_time", time);
            batch.draw(maskTexture, 0, 0, width * matrix.pixelSizeModifier,
                    height * matrix.pixelSizeModifier);
            batch.end();
        } catch (Throwable failure) {
            available = false;
            Gdx.app.error("ElementumFluid", "Smooth liquid pass disabled after error", failure);
        }
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shader != null) shader.dispose();
        if (maskTexture != null) maskTexture.dispose();
        if (maskPixmap != null) maskPixmap.dispose();
    }
}
