package com.qi.tai.opengl.base.gles.program;

import android.opengl.GLES20;
import android.util.Log;

import com.qi.tai.opengl.base.gles.BaseProgram;
import com.qi.tai.opengl.base.gles.IShaderProvider;
import com.qi.tai.opengl.base.gles.shader.ShaderProviderYUV;
import com.qi.tai.opengl.base.gles.utils.OpenGLUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * 绘制yuv
 */
public class ProgramYUV extends BaseProgram {
    private static final String TAG = ProgramYUV.class.getSimpleName();
    // handles
    private IntBuffer mYuvTextureIds = IntBuffer.wrap(new int[3]);
    private int[] mSampleHandle = new int[3];
    private int uMVPMatrix;
    private int yuvType;

    // y分量数据
    private ByteBuffer y = ByteBuffer.allocate(0);
    // u分量数据
    private ByteBuffer u = ByteBuffer.allocate(0);
    // v分量数据
    private ByteBuffer v = ByteBuffer.allocate(0);
    // uv分量数据
    private ByteBuffer uv = ByteBuffer.allocate(0);
    //yuv数据的宽高分辨率
    private int yuvWidth, yuvHeight;
    private YuvType renderYuvType = YuvType.NV21;

    public ProgramYUV() {
        super();
        OpenGLUtils.glGenTextures(3, mYuvTextureIds);
    }

    @Override
    public IShaderProvider getShaderProvider() {
        return new ShaderProviderYUV();
    }

    @Override
    public void getLocation() {
        super.getLocation();
        uMVPMatrix = GLES20.glGetAttribLocation(programHandle, "uMVPMatrix");
        yuvType = GLES20.glGetAttribLocation(programHandle, "yuvType");
    }

    public void onDraw(byte[] yuvData, int width, int height, float[] mvp, YuvType type) {
        synchronized (this) {
            if (width > 0 && height > 0) {
                if (yuvWidth != width || yuvHeight != height || renderYuvType.type != type.type) {
                    yuvWidth = width;
                    yuvHeight = height;
                    renderYuvType = type;
                    int yarraySize = width * height;
                    int uvarraySize = yarraySize / 4;
                    y = ByteBuffer.allocate(yarraySize);
                    u = ByteBuffer.allocate(uvarraySize);
                    v = ByteBuffer.allocate(uvarraySize);
                    uv = ByteBuffer.allocate(uvarraySize * 2);
                }
                switch (type){
                    case I420:
                        y.clear();
                        u.clear();
                        v.clear();
                        y.put(yuvData, 0, width * height);
                        u.put(yuvData, width * height, width * height / 4);
                        v.put(yuvData, width * height * 5 / 4, width * height / 4);
                        drawI420(width, height, mvp);
                        break;
                    case NV12:
                    case NV21:
                        y.clear();
                        uv.clear();
                        y.put(yuvData, 0, width * height);
                        uv.put(yuvData, width * height, width * height / 2);
                        drawNV21(width, height, mvp);
                        break;
                }
            }
        }
    }

    /**
     * 绘制I420
     *
     * @param mvp
     */
    private void drawI420(int width, int height, float[] mvp) {
        if (y.capacity() > 0) {
            y.position(0);
            u.position(0);
            v.position(0);
            feedTextureWithImageData(y, u, v, width, height);
            try {
                drawTexture(mvp, YuvType.I420);
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
            }
        }
    }

    /**
     * 绘制NV21
     */
    private void drawNV21(int width, int height, float[] mvp) {
        if (y.capacity() > 0) {
            y.position(0);
            uv.position(0);
            feedTextureWithImageData(y, uv, width, height);
            try {
                drawTexture(mvp, YuvType.NV21);
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
            }
        }
    }

    private int[] originalViewport = new int[4];

    /**
     * 绘制NV21
     */
    private void drawNV21(int cropX, int cropY, int viewPortWidth, int viewPortHeight, float[] mvp) {
        if (y.capacity() > 0) {
            y.position(0);
            uv.position(0);
            feedTextureWithImageData(y, uv, yuvWidth, yuvHeight);
            GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, originalViewport, 0);
            GLES20.glViewport(cropX, cropY, viewPortWidth, viewPortHeight);
            try {
                drawTexture(mvp, YuvType.NV21);
                GLES20.glViewport(originalViewport[0], originalViewport[1], originalViewport[2], originalViewport[3]);
            } catch (Exception e) {
                Log.w(TAG, e.getMessage());
            }
        }
    }

    /**
     * 绘制纹理贴图
     *
     * @param mvpMatrix 顶点坐标变换矩阵
     * @param dataType      YUV数据格式类型
     */
    private void drawTexture(float[] mvpMatrix, YuvType dataType) {
        GLES20.glUseProgram(programHandle);
        checkGlError("glUseProgram");
        /*
         * get handle for "vPosition" and "a_texCoord"
         */
        GLES20.glVertexAttribPointer(vPosition, mShaderProvider.getPointSize(), GLES20.GL_FLOAT, false, 0, mShaderProvider.getVertexBuffer());
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vPosition);

        // 4、归一化 normalized  [-1,1] . 把[2,2]转换为[-1,1]
        GLES20.glVertexAttribPointer(vCoord, mShaderProvider.getPointSize(), GLES20.GL_FLOAT, false, 0, mShaderProvider.getTextureBuffer());
        //CPU传数据到GPU，默认情况下着色器无法读取到这个数据。 需要我们启用一下才可以读取
        GLES20.glEnableVertexAttribArray(vCoord);
        // get handle to shape's transformation matrix
        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0);

        //传纹理的像素格式给fragment shader
        GLES20.glUniform1i(yuvType, dataType.type);
        //type: 0是I420, 1是NV12
        int planarCount = 0;
        switch (dataType){
            case I420:
                //I420有3个平面
                planarCount = 3;
                mSampleHandle[0] = GLES20.glGetUniformLocation(programHandle, "samplerY");
                mSampleHandle[1] = GLES20.glGetUniformLocation(programHandle, "samplerU");
                mSampleHandle[2] = GLES20.glGetUniformLocation(programHandle, "samplerV");
                break;
            case NV21:
            case NV12:
                //NV12、NV21有两个平面
                planarCount = 2;
                mSampleHandle[0] = GLES20.glGetUniformLocation(programHandle, "samplerY");
                mSampleHandle[1] = GLES20.glGetUniformLocation(programHandle, "samplerUV");
                break;
        }
        for (int i = 0; i < planarCount; i++) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
            GLES20.glBindTexture(mShaderProvider.getTextureIdTarget(), mYuvTextureIds.get(i));
            GLES20.glUniform1i(mSampleHandle[i], i);
        }

        // 调用这个函数后，vertex shader先在每个顶点执行一次，之后fragment shader在每个像素执行一次，
        // 绘制后的图像存储在render buffer中
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glFinish();

        GLES20.glDisableVertexAttribArray(vPosition);
        GLES20.glDisableVertexAttribArray(vCoord);
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量分开存储的（I420）
     *
     * @param yPlane YUV数据的Y分量
     * @param uPlane YUV数据的U分量
     * @param vPlane YUV数据的V分量
     * @param width  YUV图片宽度
     * @param height YUV图片高度
     */
    private void feedTextureWithImageData(ByteBuffer yPlane, ByteBuffer uPlane, ByteBuffer vPlane, int width, int height) {
        //根据YUV编码的特点，获得不同平面的基址
        textureYUV(yPlane, width, height, 0);
        textureYUV(uPlane, width / 2, height / 2, 1);
        textureYUV(vPlane, width / 2, height / 2, 2);
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量交叉存储的（NV12、NV21）
     *
     * @param yPlane  YUV数据的Y分量
     * @param uvPlane YUV数据的UV分量
     * @param width   YUV图片宽度
     * @param height  YUV图片高度
     */
    private void feedTextureWithImageData(ByteBuffer yPlane, ByteBuffer uvPlane, int width, int height) {
        //根据YUV编码的特点，获得不同平面的基址
        textureYUV(yPlane, width, height, 0);
        textureNV12(uvPlane, width / 2, height / 2, 1);
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量分开存储的（I420）
     *
     * @param imageData YUV数据的Y/U/V分量
     * @param width     YUV图片宽度
     * @param height    YUV图片高度
     */
    private void textureYUV(ByteBuffer imageData, int width, int height, int index) {
        // 将纹理对象绑定到纹理目标
        GLES20.glBindTexture(mShaderProvider.getTextureIdTarget(), mYuvTextureIds.get(index));
        // 设置放大和缩小时，纹理的过滤选项为：线性过滤
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // 设置纹理X,Y轴的纹理环绕选项为：边缘像素延伸
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        // 加载图像数据到纹理，GL_LUMINANCE指明了图像数据的像素格式为只有亮度，虽然第三个和第七个参数都使用了GL_LUMINANCE，
        // 但意义是不一样的，前者指明了纹理对象的颜色分量成分，后者指明了图像数据的像素格式
        // 获得纹理对象后，其每个像素的r,g,b,a值都为相同，为加载图像的像素亮度，在这里就是YUV某一平面的分量值
        GLES20.glTexImage2D(
                mShaderProvider.getTextureIdTarget(), 0,
                GLES20.GL_LUMINANCE, width, height, 0,
                GLES20.GL_LUMINANCE,
                GLES20.GL_UNSIGNED_BYTE, imageData
        );
    }

    /**
     * 将图片数据绑定到纹理目标，适用于UV分量交叉存储的（NV12、NV21）
     *
     * @param imageData YUV数据的UV分量
     * @param width     YUV图片宽度
     * @param height    YUV图片高度
     */
    private void textureNV12(ByteBuffer imageData, int width, int height, int index) {
        GLES20.glBindTexture(mShaderProvider.getTextureIdTarget(), mYuvTextureIds.get(index));
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(mShaderProvider.getTextureIdTarget(), GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexImage2D(
                mShaderProvider.getTextureIdTarget(), 0,
                GLES20.GL_LUMINANCE_ALPHA, width, height, 0,
                GLES20.GL_LUMINANCE_ALPHA,
                GLES20.GL_UNSIGNED_BYTE, imageData
        );
    }


    /**
     * 检查GL操作是否有error
     *
     * @param op 检查当前所做的操作
     */
    private void checkGlError(String op) {
        int error = GLES20.glGetError();
        while (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "***** $op: glError $error  " + op);
            error = GLES20.glGetError();
        }
    }
    /**
     * 绘制的类型
     */
    public enum YuvType{
        I420(0),
        NV12(1),
        NV21(2);
        int type;
        YuvType(int type){
            this.type = type;
        }
    }
}
