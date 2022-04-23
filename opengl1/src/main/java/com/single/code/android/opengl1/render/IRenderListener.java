package com.single.code.android.opengl1.render;

import android.graphics.SurfaceTexture;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public interface IRenderListener {
    void onSurfaceCreated(GL10 gl, EGLConfig config);

    void onSurfaceChanged(GL10 gl, int width, int height);

    void onDrawFrame(final int textureId, float[] mtx, SurfaceTexture surfaceTexture);

    void onSurfaceDestroyed();
}
