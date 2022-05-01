package com.single.code.android.opengl1.record;

import com.single.code.android.opengl1.gles.BaseProgram;
import com.single.code.android.opengl1.gles.shader.ShaderProviderScreen2D;
import com.single.code.android.opengl1.gles.IShaderProvider;

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
