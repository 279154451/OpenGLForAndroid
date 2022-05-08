package com.qi.tai.opengl.base.render;

import android.graphics.SurfaceTexture;
import android.opengl.EGLConfig;


public interface IRenderStatusListener {
    void onSurfaceCreated( EGLConfig config);

    void onSurfaceChanged(int width, int height);

    void onDrawFrame(final int textureId, float[] mtx, SurfaceTexture surfaceTexture);

    void onSurfaceDestroyed();
}
