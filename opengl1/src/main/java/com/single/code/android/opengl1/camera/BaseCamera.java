package com.single.code.android.opengl1.camera;

import com.single.code.android.opengl1.surface.GLView;

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

    public abstract void startPreview();
    public abstract void stopPreview();

}
