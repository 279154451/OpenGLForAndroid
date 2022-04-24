package com.single.code.android.opengl1.surface;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public interface GLView {
    void requestRender();

    LifecycleOwner getLifecycleOwner();

    Context getContext();
}
