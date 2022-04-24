package com.single.code.android.opengl1.camera;

import android.graphics.SurfaceTexture;

/**
 * 创建时间：2022/4/23
 * 创建人：singleCode
 * 功能描述：
 **/
public interface IPreviewOutputUpdateListener {
    void onUpdate(SurfaceTexture texture);
}
