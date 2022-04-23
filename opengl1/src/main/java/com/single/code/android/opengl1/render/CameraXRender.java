package com.single.code.android.opengl1.render;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Environment;


import androidx.camera.core.Preview;
import androidx.core.content.ContextCompat;

import com.single.code.android.opengl1.program.BaseProgram;
import com.single.code.android.opengl1.program.CameraFBOProgram;
import com.single.code.android.opengl1.program.ScreenProgram;
import com.single.code.android.opengl1.record.MediaRecorder;
import com.single.code.android.opengl1.record.RecordManager;
import com.single.code.android.opengl1.surface.ISurface;

import java.io.File;

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
    private BaseProgram cameraProgram;//fbo着色器程序
    private BaseProgram screenProgram;//屏幕着色器程序
    private IRenderListener listener;
    public CameraXRender(ISurface surface){
        this.surface = surface;
        textureId = new int[1];
        listener = RecordManager.getManager();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        cameraProgram = new CameraFBOProgram();
        screenProgram = new ScreenProgram();
        surfaceTexture.attachToGLContext(textureId[0]);//将摄像头的数据与创建的纹理关联，attachToGLContext函数内部会调用GLES20.glGenTextures(textures.length, textures, 0);来创建纹理
        surfaceTexture.setOnFrameAvailableListener(this);
        if(listener != null){
            listener.onSurfaceCreated(gl,config);
        }
    }
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        cameraProgram.surfaceChange(width,height);
        screenProgram.surfaceChange(width,height);
        if(listener != null){
            listener.onSurfaceChanged(gl,width,height);
        }
    }
    float[] mtx = new float[16];
    @Override
    public void onDrawFrame(GL10 gl) {
        if(surfaceTexture!=null){
            surfaceTexture.updateTexImage();//更新纹理
            surfaceTexture.getTransformMatrix(mtx);//获得纹理矩阵
            int fboText = cameraProgram.onDraw(textureId[0],mtx);
            fboText=  screenProgram.onDraw(fboText,mtx);
            if(listener != null){
                listener.onDrawFrame(fboText,mtx,surfaceTexture);
            }
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
    public void surfaceDestroyed(){
        if(listener != null){
            listener.onSurfaceDestroyed();
        }
    }
}
