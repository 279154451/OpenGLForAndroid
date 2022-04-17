package com.single.code.android.opengl1.render;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;


import androidx.camera.core.Preview;

import com.single.code.android.opengl1.program.CameraXProgram;
import com.single.code.android.opengl1.program.CameraXProgramOES;
import com.single.code.android.opengl1.program.OpenGLUtils;
import com.single.code.android.opengl1.surface.ISurface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraXRender implements GLSurfaceView.Renderer, Preview.OnPreviewOutputUpdateListener,SurfaceTexture.OnFrameAvailableListener {
    private ISurface surface;
    private int[] textureId;
    private SurfaceTexture surfaceTexture;
    private CameraXProgramOES program;//着色器程序
    public CameraXRender(ISurface surface){
        this.surface = surface;
        textureId = new int[1];
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = new CameraXProgramOES(surface.getContext());
//        program.glGenTextures(textureId);
        surfaceTexture.attachToGLContext(textureId[0]);//将摄像头的数据与创建的纹理关联，attachToGLContext函数内部会调用GLES20.glGenTextures(textures.length, textures, 0);来创建纹理
        surfaceTexture.setOnFrameAvailableListener(this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        program.glViewport(width,height);

    }
    float[] mtx = new float[16];
    @Override
    public void onDrawFrame(GL10 gl) {
        if(surfaceTexture!=null){
            surfaceTexture.updateTexImage();//更新纹理
            surfaceTexture.getTransformMatrix(mtx);//获得纹理矩阵
            program.onDraw(textureId[0],mtx);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if(surface!= null){
            surface.requestRender();
        }
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        surfaceTexture = output.getSurfaceTexture();
    }
}
