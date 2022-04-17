package com.single.code.android.opengl1.camera;

import com.single.code.android.opengl1.surface.ISurface;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class BaseCamera {
    protected ISurface surface;
    public BaseCamera(ISurface surface){
        this.surface = surface;
    }

}
