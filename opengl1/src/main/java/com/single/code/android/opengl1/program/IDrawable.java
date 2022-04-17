package com.single.code.android.opengl1.program;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public interface IDrawable {
    int getTextureIdTarget();
    String getVertexShader();//获取顶点着色器程序代码
    String getFragmentShader();//获取片元着色器程序代码
}
