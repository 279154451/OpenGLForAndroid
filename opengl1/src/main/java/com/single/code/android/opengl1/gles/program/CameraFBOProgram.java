package com.single.code.android.opengl1.gles.program;



import com.single.code.android.opengl1.gles.shader.ShaderProviderOES;
import com.single.code.android.opengl1.gles.IShaderProvider;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：OES ->FBO 离屏渲染
 **/
public class CameraFBOProgram extends FBOProgram{


    @Override
    public IShaderProvider getShaderProvider() {
        return new ShaderProviderOES();
    }

}
