package com.qi.tai.opengl.base.gles.program;


import com.qi.tai.opengl.base.gles.shader.ShaderProviderScreen2D;
import com.qi.tai.opengl.base.gles.BaseProgram;
import com.qi.tai.opengl.base.gles.IShaderProvider;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：将FBO的纹理重新渲染到屏幕上
 **/
public class ScreenProgram extends BaseProgram {


    @Override
    public IShaderProvider getShaderProvider() {
        return new ShaderProviderScreen2D();
    }

    @Override
    public void beforeDraw(int texture, float[] mtx) {
        //这里不用进行矩阵变换，因为在之前已经变换过了
    }
}
