package com.single.code.android.opengl1.program;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.single.code.android.opengl1.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraXProgramOES {
    private  final int vPosition;
    private final int vCoord;
    private final int vTexture;
    private final int vMatrix;
    private int programHandle;
    private FloatBuffer vertexBuffer;//顶点坐标缓冲区
    private FloatBuffer textureBuffer;//纹理坐标缓冲区
    public CameraXProgramOES(Context context) {
        //4个点，每个点是flot类型，flot类型占4个字节，每个点有x,y两个坐标，所以缓存区大小为：4*4*2，其中order(ByteOrder.nativeOrder())是设置字节序的大小端
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
//可以通过旋转纹理坐标来达到画面旋转和镜像效果
        textureBuffer.clear();
        textureBuffer.put(TEXTURE);


        String vertexSharder = OpenGLUtils.readRawTextFile(context, R.raw.camera_vert);//顶点着色器
        String fragSharder = OpenGLUtils.readRawTextFile(context, R.raw.camera_frag);//片元着色器
        //着色器程序准备好
        programHandle = OpenGLUtils.loadProgram(vertexSharder, fragSharder);

        //获取程序中的变量 索引
        vPosition = GLES20.glGetAttribLocation(programHandle, "vPosition");//这里的name对应着色器程序中定义的变量名称
        vCoord = GLES20.glGetAttribLocation(programHandle, "vCoord");
        vTexture = GLES20.glGetUniformLocation(programHandle, "vTexture");
        vMatrix = GLES20.glGetUniformLocation(programHandle, "vMatrix");
    }


    public void glViewport(int width, int height) {
        //设置绘制区域
        GLES20.glViewport(0, 0, width, height);
    }
    public static void glGenTextures(int[] textures) {
        GLES20.glGenTextures(textures.length, textures, 0);
        for (int i = 0; i < textures.length; i++) {
            //摄像头是外部纹理 external oes
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[i]);

            /**
             *  必须：设置纹理过滤参数设置
             */
            /*设置纹理缩放过滤*/
            // GL_NEAREST: 使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
            // GL_LINEAR:  使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
            // 后者速度较慢，但视觉效果好
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);//放大过滤
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_LINEAR);//缩小过滤

            /**
             * 可选：设置纹理环绕方向
             */
            //纹理坐标的范围是0-1。超出这一范围的坐标将被OpenGL根据GL_TEXTURE_WRAP参数的值进行处理
            //GL_TEXTURE_WRAP_S, GL_TEXTURE_WRAP_T 分别为x，y方向。
            //GL_REPEAT:平铺
            //GL_MIRRORED_REPEAT: 纹理坐标是奇数时使用镜像平铺
            //GL_CLAMP_TO_EDGE: 坐标超出部分被截取成0、1，边缘拉伸
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
//                    GLES20.GL_CLAMP_TO_EDGE);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
//                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,0);
        }
    }
    public void onDraw(int texture,float[] mtx) {
        GLES20.glUseProgram(programHandle);
//        GLES20.glClearColor(0,0,0,0);
//        GLES20.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        vertexBuffer.position(0);
        // 4、size=2绘制的是x,y两个坐标，归一化 normalized  [-1,1] . 如果坐标点传的是[2,2]超出了坐标系范围，归一化就会将[2,2]转换为[-1,1]之间
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vPosition);


        textureBuffer.position(0);
        // 4、归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);


        //相当于激活一个用来显示图片的画框（图层）
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture);
        // 0: 图层ID  GL_TEXTURE0
        // GL_TEXTURE1 ， 1
        GLES20.glUniform1i(vTexture,0);//将激活的图层传递给片元着色器。所以第二个参数值：激活GLES20.GL_TEXTURE0就是0，GLES20.GL_TEXTURE1就是1


        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);

        //通知画画，
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);//count:绘制四个点

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,0);

    }

}
