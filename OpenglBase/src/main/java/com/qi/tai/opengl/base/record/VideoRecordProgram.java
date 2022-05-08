package com.qi.tai.opengl.base.record;

import com.qi.tai.opengl.base.gles.BaseProgram;
import com.qi.tai.opengl.base.gles.shader.ShaderProviderScreen2D;
import com.qi.tai.opengl.base.gles.IShaderProvider;

public class VideoRecordProgram extends BaseProgram {

    @Override
    public IShaderProvider getShaderProvider() {
        return new ShaderProviderScreen2D();
    }

    @Override
    public void beforeDraw(int texture, float[] mtx) {
        //这里不用进行矩阵变换，因为在之前已经变换过了
    }
}
