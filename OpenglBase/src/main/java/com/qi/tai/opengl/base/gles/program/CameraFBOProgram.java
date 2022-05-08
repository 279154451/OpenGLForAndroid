package com.qi.tai.opengl.base.gles.program;



import com.qi.tai.opengl.base.gles.shader.ShaderProviderOES;
import com.qi.tai.opengl.base.gles.IShaderProvider;

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
