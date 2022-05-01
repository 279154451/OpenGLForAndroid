package com.single.code.android.opengl1.gles.program;



import com.single.code.android.opengl1.gles.shader.ShaderProviderOES;
import com.single.code.android.opengl1.gles.BaseProgram;
import com.single.code.android.opengl1.gles.IShaderProvider;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public class ProgramOES extends BaseProgram {
    @Override
    public IShaderProvider getShaderProvider() {
        return new ShaderProviderOES();
    }

}
