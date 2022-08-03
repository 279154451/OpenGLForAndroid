package com.qi.tai.opengl.base.gles.utils;

import android.opengl.Matrix;

/**
 * 创建时间：2022/5/29
 * 创建人：singleCode
 * 功能描述：
 **/
public class MatrixUtils {
    /**
     * 通过宽高比计算出投影矩阵，再将 投影矩阵*顶点坐标（矩阵*向量） 得到物体真实的归一化设备坐标，从而解决OpenGL的宽高比问题
     * 算法：
     *  2/right-left        0                 0         -(right+left)/(right-left)
     *
     *      0          2/(top-bottom)         0         -(top+bottom)/(top-bottom)
     *
     *      0              0            -2/(far-near)   -(far+near)/(far-near)
     *
     *      0              0                  0                   1
     *
     * @param width 视频的宽
     * @param height 视频的高
     * @return 正交投影矩阵
     */
    public static float[] otherM(int width,int height){
        float[] projectionMatrix = new float[16];//正交投影矩阵
        Matrix.setIdentityM(projectionMatrix,0);//将正交矩阵先初始化为单位矩阵
        if(width > height){
            float aspectRatio = (float)width/(float)height;
            //宽>高:以y轴纹理坐标范围（-1，1）为基准，按宽高比缩放x轴纹理坐标范围（-aspectRatio,aspectRatio），z轴纹理坐标范围不变（-1，1）
            Matrix.orthoM(projectionMatrix,0,-aspectRatio,aspectRatio,-1,1,-1,1);
        }else {
            float aspectRatio = (float)height/(float)width;
            //高大于宽:以x轴纹理坐标范围（-1，1）为基准，按高宽比缩放y轴纹理坐标范围（-aspectRatio,aspectRatio），z轴纹理坐标范围不变（-1，1）
            Matrix.orthoM(projectionMatrix,0,-1,1,-aspectRatio,aspectRatio,-1,1);
        }

        return projectionMatrix;
    }

    /**
     *得到一个在x,y,z轴平移指定大小的平移举证
     * @param x x轴平移值
     * @param y y轴平移值
     * @param z z轴平移值
     * @return 平移后的向量坐标
     */

    public static float[] translationM(float x,float y,float z){
        float[] translationMatrix = new float[16];
        Matrix.setIdentityM(translationMatrix,0);//初始化为单位矩阵
        Matrix.translateM(translationMatrix,0,x,y,z);
        return translationMatrix;
    }

    /**
     * 得到透视投影矩阵
     * 算法：
     *   a/aspect     0          0               0
     *
     *     0          a          0               0
     *
     *     0          0     -(f+n)/(f-n)    -2fn/(f-n)
     *
     *     0          0         -1              0
     *
     * @param fovy 视角的角度（如：90°、45°。。）
     * @param aspect 屏幕的宽高比（width/height）
     * @param n 焦点到近平面的距离，必须是正值，（如 n=1,表示近平面就位于一个z轴值为-1的位置）
     * @param f 焦点到远平面的距离，必须是正值，且大于到近平面的距离（f>n）
     * @return
     */
    public static float[] perspectiveM(float fovy,float aspect,float n,float f){
        float[] perspectiveMatrix = new float[16];//透视投影矩阵
        /**
         * angleRadians 表示视野，视野=视角*Math.PI/180.0,也就是视角范围内可见的扇形角度
         */
        float angleRadians = (float)(fovy * Math.PI/180.0);
        /**
         * a就表示焦距（焦点到近平面的距离），焦距 =1/tan(视野/2)
         */
        float a = (float) (1.0/Math.tan(angleRadians/2.0));

        int offset = 0;
        perspectiveMatrix[offset + 0] = a / aspect;
        perspectiveMatrix[offset + 1] = 0.0f;
        perspectiveMatrix[offset + 2] = 0.0f;
        perspectiveMatrix[offset + 3] = 0.0f;

        perspectiveMatrix[offset + 4] = 0.0f;
        perspectiveMatrix[offset + 5] = a;
        perspectiveMatrix[offset + 6] = 0.0f;
        perspectiveMatrix[offset + 7] = 0.0f;

        perspectiveMatrix[offset + 8] = 0.0f;
        perspectiveMatrix[offset + 9] = 0.0f;
        perspectiveMatrix[offset + 10] =-((f+n)/(f-n));
        perspectiveMatrix[offset + 11] = -1.0f;

        perspectiveMatrix[offset + 12] = 0.0f;
        perspectiveMatrix[offset + 13] = 0.0f;
        perspectiveMatrix[offset + 14] = -((2.0f*f*n)/(f-n));
        perspectiveMatrix[offset + 15] = 0.0f;
//        Matrix.perspectiveM(projectionMatrix,0,fovy,aspect,n,f);
        return perspectiveMatrix;
    }
    

}
