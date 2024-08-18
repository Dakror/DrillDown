/*******************************************************************************
 * Copyright 2018 Maximilian Stark | Dakror <mail@dakror.de>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.dakror.quarry.util;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;

import de.dakror.common.libgdx.render.DepthSpriter;

/**
 * @author Maximilian Stark | Dakror
 */
public class SpriterDelegateBatch implements Batch {
    DepthSpriter spriter;

    public SpriterDelegateBatch(DepthSpriter spriter) {
        this.spriter = spriter;
    }

    @Override
    public void dispose() {
        spriter.dispose();
    }

    @Override
    public void begin() {
        spriter.begin(true);
    }

    @Override
    public void end() {
        spriter.end();
    }

    @Override
    public void setColor(Color tint) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Color getColor() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setPackedColor(float packedColor) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public float getPackedColor() {
        throw new RuntimeException("not implemented");
    }

    float z;

    public void setNextZ(float z) {
        this.z = z;
    }

    @Override
    public void draw(Texture texture, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY,
            float rotation, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(Texture texture, float x, float y) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(Texture texture, float x, float y, float width, float height) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(Texture texture, float[] spriteVertices, int offset, int count) {
        if (spriter.getIdx() + 4 * DepthSpriter.VERTEX_SIZE >= spriter.getVertexCount())
            flush();
        int i = 0;

        float[] vert = spriter.getVertices();

        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];
        i++;
        vert[spriter.inc()] = z;
        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];

        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];
        i++;
        vert[spriter.inc()] = z;
        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];

        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];
        i++;
        vert[spriter.inc()] = z;
        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];

        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];
        i++;
        vert[spriter.inc()] = z;
        vert[spriter.inc()] = spriteVertices[i++];
        vert[spriter.inc()] = spriteVertices[i++];

        //        for (int j = 25; j > 0; j--) {
        //            System.out.printf(Locale.ENGLISH, "%f, ", vert[spriter.getIdx() - j]);
        //        }
        //        System.out.println();
    }

    @Override
    public void draw(TextureRegion region, float x, float y) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float width, float height) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void draw(TextureRegion region, float width, float height, Affine2 transform) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void flush() {
        spriter.flush();
    }

    @Override
    public void disableBlending() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void enableBlending() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setBlendFunction(int srcFunc, int dstFunc) {}

    @Override
    public void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {}

    @Override
    public int getBlendSrcFunc() {
        return 0;
    }

    @Override
    public int getBlendDstFunc() {
        return 0;
    }

    @Override
    public int getBlendSrcFuncAlpha() {
        return 0;
    }

    @Override
    public int getBlendDstFuncAlpha() {
        return 0;
    }

    @Override
    public Matrix4 getProjectionMatrix() {
        return null;
    }

    @Override
    public Matrix4 getTransformMatrix() {
        return null;
    }

    @Override
    public void setProjectionMatrix(Matrix4 projection) {
        spriter.setProjectionMatrix(projection);
    }

    @Override
    public void setTransformMatrix(Matrix4 transform) {}

    @Override
    public void setShader(ShaderProgram shader) {}

    @Override
    public ShaderProgram getShader() {
        return spriter.getShader();
    }

    @Override
    public boolean isBlendingEnabled() {
        return false;
    }

    @Override
    public boolean isDrawing() {
        return false;
    }

}
