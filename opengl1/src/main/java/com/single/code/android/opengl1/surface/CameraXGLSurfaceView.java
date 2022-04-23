package com.single.code.android.opengl1.surface;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import androidx.lifecycle.LifecycleOwner;

import com.single.code.android.opengl1.camera.BaseCamera;
import com.single.code.android.opengl1.camera.CameraXHelper;
import com.single.code.android.opengl1.render.CameraXRender;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraXGLSurfaceView extends GLSurfaceView implements ISurface{
    private CameraXHelper camera;
    private CameraXRender renderer;
    public CameraXGLSurfaceView(Context context) {
       this(context,null);
    }

    public CameraXGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    private void initView(){
        camera = new CameraXHelper(this);
        renderer = new CameraXRender(this);
        camera.startPreview(renderer);//开启预览
        setEGLContextClientVersion(2);//使用OpenGL ES 2.0 context.
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);//需要调用requestRender来触发一次render的绘制

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        renderer.surfaceDestroyed();
    }

    @Override
    public void requestRender() {
        super.requestRender();
    }

    @Override
    public LifecycleOwner getLifecycleOwner() {
        return (LifecycleOwner) getContext();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        camera.stopPreview();
    }
}
