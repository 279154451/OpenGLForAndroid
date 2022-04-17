package com.single.code.android.opengl1.program;

import android.opengl.GLES11Ext;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public class DrawableOES implements IDrawable {
    private final static String VertexShader =
            "attribute vec4 vPosition; //变量 float[4]  一个顶点  java传过来的\n" +
            "attribute vec4 vCoord;  //纹理坐标\n" +
            "//attribute vec2 vCoord;  //纹理坐标\n" +
            "varying vec2 aCoord;//传递给片元着色器的变量，varying修饰，片元着色器程序中需要有个一摸一样的变量定义\n" +
            "uniform mat4 vMatrix;\n" +
            "//顶点着色器,用来确定要绘制的几何体形状\n" +
            "void main(){\n" +
            "    //内置变量： 把坐标点赋值给gl_position 就Ok了。\n" +
            "    gl_Position = vPosition;\n" +
            "//    aCoord = (vMatrix * vec4(vCoord,1.0,1.0)).xy;\n" +
            "    aCoord = (vMatrix * vCoord).xy;\n" +
            "}\n";
    private final static String FragmentShader =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "//摄像头数据比较特殊的一个地方\n" +
                    "precision mediump float; // 数据精度\n" +
                    "varying vec2 aCoord;\n" +
                    "uniform samplerExternalOES  vTexture;  // samplerExternalOES: 图片， 采样器\n" +
                    "//片元着色器，用来绘制上色\n" +
                    "void main(){\n" +
                    "    //  texture2D: vTexture采样器，采样  aCoord 这个像素点的RGBA值\n" +
                    "        gl_FragColor =texture2D(vTexture,aCoord);\n" +
                    "//    vec4 rgba = texture2D(vTexture,aCoord);  //rgba\n" +
                    "//    float r = 0.33*rgba.a+0.59*rgba.g+0.11*rgba.b;//这里是利用305911公式来达到灰度化效果\n" +
                    "//    gl_FragColor = vec4(r,r,r,rgba.a);\n" +
                    "}\n";
    @Override
    public int getTextureIdTarget() {
        return GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

    @Override
    public String getVertexShader() {
        return VertexShader;
    }

    @Override
    public String getFragmentShader() {
        return FragmentShader;
    }
}
