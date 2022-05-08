package com.qi.tai.opengl.base.gles;

import android.opengl.GLES20;

import com.qi.tai.opengl.base.gles.utils.OpenGLUtils;

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
    protected IShaderProvider mShaderProvider;
    public abstract IShaderProvider getShaderProvider();
    public BaseProgram() {
        mShaderProvider = getShaderProvider();
        initGL();
    }
    public void initGL(){
        //初始化着色器程序
        initProgram();
        //获取程序中的变量 索引
        getLocation();
    }
    public void initProgram(){
        String vertexSharder = mShaderProvider.getVertexShader();//顶点着色器
        String fragSharder = mShaderProvider.getFragmentShader();//片元着色器
        //着色器程序准备好
        programHandle = OpenGLUtils.loadProgram(vertexSharder, fragSharder);
    }

    public void getLocation(){
        //具体使用glGetAttribLocation还是glGetUniformLocation来获取变量位置，需要根据着色器程序的变量定义来定
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
        GLES20.glClearColor(0f,0f,0f,0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, mWidth, mHeight);

        GLES20.glUseProgram(programHandle);

        // 4、归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]
        /**
         * indx:着色器读取顶点数据后缓存的变量地址
         * size:每个顶点数据由几个分量组成
         * type:顶点数据类型
         * stride:从顶点数组缓冲区读取数据时每读一个顶点的跨距（也就是顶点p1和p2之间需要跨多少字节读取）
         * ptr:顶点数组缓冲区
         */
        GLES20.glVertexAttribPointer(vPosition, mShaderProvider.getPointSize(), GLES20.GL_FLOAT, false, 0, mShaderProvider.getVertexBuffer());
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vPosition);

        // 4、归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]
        GLES20.glVertexAttribPointer(vCoord, mShaderProvider.getPointSize(), GLES20.GL_FLOAT, false, 0, mShaderProvider.getTextureBuffer());
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);


        //相当于激活一个用来显示图片的画框
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mShaderProvider.getTextureIdTarget(),texture);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
        // 0: 图层ID  GL_TEXTURE0
        // GL_TEXTURE1 ， 1
        GLES20.glUniform1i(vTexture,0);


        beforeDraw(texture,mtx);

        /*
         *通知画画
         * mode=GLES20.GL_TRIANGLE_STRIP表示绘制三角形
         * first=0表示从顶点数组的开头开始读取顶点
         * count=4表示告诉OpenGL读如4个顶点，这样会绘制两个三角形（p1-p2-p3三角形和p2-p3-p4三角形），从而组成一个矩形了
         */
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,0);
        GLES20.glBindTexture(mShaderProvider.getTextureIdTarget(),0);
        return texture;
    }

    public  void beforeDraw(int texture,float[] mtx){
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);
    }
    public void release(){
        GLES20.glDeleteProgram(programHandle);
    }
}
