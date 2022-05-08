package com.qi.tai.opengl.base.render;

import android.opengl.GLSurfaceView;

import com.qi.tai.opengl.base.surface.GLView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public abstract class BaseRender implements GlRender, GLSurfaceView.Renderer {
    protected GLView glView;

    public BaseRender(GLView glView) {
        this.glView = glView;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        onSurfaceChanged(width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        onSurfaceCreated(null);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        onDrawFrame();
    }

    @Override
    public void onSurfaceDestroy() {
        if (glView != null) {
            glView.onPause();
        }
    }

}
