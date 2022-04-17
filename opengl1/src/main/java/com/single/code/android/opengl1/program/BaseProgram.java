package com.single.code.android.opengl1.program;

import android.opengl.GLES20;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public abstract class BaseProgram {
    protected   int vPosition;
    protected  int vCoord;
    protected  int vTexture;
    protected  int vMatrix;
    protected int programHandle;
    protected FloatBuffer vertexBuffer;//顶点坐标缓冲区
    protected FloatBuffer textureBuffer;//纹理坐标缓冲区
    protected IDrawable mDrawable;
    public abstract IDrawable getDrawable();
    public BaseProgram() {
        mDrawable = getDrawable();
        initGL();
    }
    public void initGL(){
        vertexBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        float[] VERTEX = {//世界坐标的原点在画布中间
                -1.0f, -1.0f,//左下
                1.0f, -1.0f,//右下
                -1.0f, 1.0f,//左上
                1.0f, 1.0f//右上
        };
        vertexBuffer.clear();
        vertexBuffer.put(VERTEX);


        textureBuffer = ByteBuffer.allocateDirect(4 * 4 * 2).order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        float[] TEXTURE = {//纹理坐标的原点在左下角
                0.0f, 0.0f,//左下
                1.0f, 0.0f,//右下
                0.0f, 1.0f,//左上
                1.0f, 1.0f//右上
        };

        textureBuffer.clear();
        textureBuffer.put(TEXTURE);


        String vertexSharder = mDrawable.getVertexShader();//顶点着色器
        String fragSharder = mDrawable.getFragmentShader();//片元着色器
        //着色器程序准备好
        programHandle = OpenGLUtils.loadProgram(vertexSharder, fragSharder);

        //获取程序中的变量 索引
        vPosition = GLES20.glGetAttribLocation(programHandle, "vPosition");
        vCoord = GLES20.glGetAttribLocation(programHandle, "vCoord");
        vTexture = GLES20.glGetUniformLocation(programHandle, "vTexture");
        vMatrix = GLES20.glGetUniformLocation(programHandle, "vMatrix");
    }

    private int mWidth,mHeight;
    public void surfaceChange(int width, int height) {
        mWidth = width;
        mHeight = height;
    }
    public int onDraw(int texture,float[] mtx) {
        //设置绘制区域
        GLES20.glViewport(0, 0, mWidth, mHeight);

        GLES20.glUseProgram(programHandle);

        vertexBuffer.position(0);
        // 4、归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vPosition);


        textureBuffer.position(0);
        // 4、归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);


        //相当于激活一个用来显示图片的画框
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
        // 0: 图层ID  GL_TEXTURE0
        // GL_TEXTURE1 ， 1
        GLES20.glUniform1i(vTexture,0);


        beforeDraw(texture,mtx);

        //通知画画，
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        return texture;
    }

    public  void beforeDraw(int texture,float[] mtx){

    }
    public void release(){
        GLES20.glDeleteProgram(programHandle);
    }
}
