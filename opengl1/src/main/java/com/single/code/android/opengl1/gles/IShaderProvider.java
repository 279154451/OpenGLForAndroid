package com.single.code.android.opengl1.gles;

import com.single.code.android.opengl1.gles.utils.OpenGLUtils;

import java.nio.FloatBuffer;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public abstract class IShaderProvider {
    protected FloatBuffer vertexBuffer;//顶点坐标缓冲区
    protected FloatBuffer textureBuffer;//纹理坐标缓冲区
    public abstract int getTextureIdTarget();//GLES20.GL_TEXTURE_2D or  GLES11Ext.GL_TEXTURE_EXTERNAL_OES
    public  abstract String getVertexShader();//获取顶点着色器程序代码
    public abstract String getFragmentShader();//获取片元着色器程序代码

    /**
     *
     * @param VERTEX 世界坐标系中各顶点的坐标
     * @param TEXTURE 纹理坐标系中各顶点的坐标
     */
    public IShaderProvider( float[] VERTEX,float[] TEXTURE ){
        textureBuffer = OpenGLUtils.createFloatBuffer(TEXTURE);//将定义的顶点坐标位置复制到一个缓冲区中，等待OpenGL从这个缓冲区中读取顶点位置
        vertexBuffer = OpenGLUtils.createFloatBuffer(VERTEX);
    }

    public FloatBuffer getTextureBuffer() {
        textureBuffer.position(0);//确保OpenGl是从缓冲区的起始位置读数据
        return textureBuffer;
    }

    public int getPointSize(){
        return 2;//表示每个顶点由两个分量组成（x和y坐标）
    }

    public FloatBuffer getVertexBuffer() {
        vertexBuffer.position(0);
        return vertexBuffer;
    }
}
