package com.single.code.android.opengl1.surface;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import androidx.lifecycle.LifecycleOwner;

import com.qi.tai.opengl.base.camera.BaseCamera;
import com.qi.tai.opengl.base.camera.Camera2Helper;
import com.qi.tai.opengl.base.render.CameraRender;
import com.qi.tai.opengl.base.surface.GLView;
import com.single.code.android.opengl1.utils.Constants;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraGLSurfaceView extends GLSurfaceView implements GLView {
    private BaseCamera camera;
    private CameraRender renderer;
    public CameraGLSurfaceView(Context context) {
       this(context,null);
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }
    private void initView(){
        renderer = new CameraRender(this);
//        camera = new CameraXHelper(this,renderer);
        camera = new Camera2Helper(this,renderer);
        camera.startPreview(Constants.mCameraWidth,Constants.mCameraHeight);//开启预览
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
        renderer.onSurfaceDestroy();
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
