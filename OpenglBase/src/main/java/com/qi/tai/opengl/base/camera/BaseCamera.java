package com.qi.tai.opengl.base.camera;


import com.qi.tai.opengl.base.surface.GLView;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public abstract class BaseCamera {
    protected GLView glView;
    protected IPreviewOutputUpdateListener listener;
    public BaseCamera(GLView glView,IPreviewOutputUpdateListener listener){
        this.glView = glView;
        this.listener = listener;
    }

    public abstract void startPreview(int width,int height);
    public abstract void stopPreview();

}
