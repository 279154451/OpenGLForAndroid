package com.single.code.android.opengl1.program;


/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：将FBO的纹理重新渲染到屏幕上
 **/
public class ScreenProgram extends BaseProgram {

    @Override
    public IDrawable getDrawable() {
        return new DrawableScreen2D();
    }

    @Override
    public void beforeDraw(int texture, float[] mtx) {
        //这里不用进行矩阵变换，因为在之前已经变换过了
    }
}
