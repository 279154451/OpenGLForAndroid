package com.single.code.android.opengl1.render;

import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;



import com.single.code.android.opengl1.camera.IPreviewOutputUpdateListener;
import com.single.code.android.opengl1.program.BaseProgram;
import com.single.code.android.opengl1.program.CameraFBOProgram;
import com.single.code.android.opengl1.program.ScreenProgram;
import com.single.code.android.opengl1.record.RecordManager;
import com.single.code.android.opengl1.surface.GLView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraRender implements GLSurfaceView.Renderer, IPreviewOutputUpdateListener,SurfaceTexture.OnFrameAvailableListener {
    private GLView glView;
    private int[] textureId;
    private SurfaceTexture surfaceTexture;
    private BaseProgram cameraProgram;//fbo着色器程序
    private BaseProgram screenProgram;//屏幕着色器程序
    private IRenderListener listener;
    public CameraRender(GLView glView){
        this.glView = glView;
        textureId = new int[1];
        listener = RecordManager.getManager();
    }
    private volatile boolean attachToGlContext =false;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        cameraProgram = new CameraFBOProgram();
        screenProgram = new ScreenProgram();
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
            if(!attachToGlContext){
                surfaceTexture.attachToGLContext(textureId[0]);//将摄像头的数据与创建的纹理关联，attachToGLContext函数内部会调用GLES20.glGenTextures(textures.length, textures, 0);来创建纹理
                attachToGlContext = true;
                glView.requestRender();
            }else {
                surfaceTexture.updateTexImage();//更新纹理
                surfaceTexture.getTransformMatrix(mtx);//获得纹理矩阵
                int fboText = cameraProgram.onDraw(textureId[0],mtx);
                fboText=  screenProgram.onDraw(fboText,mtx);
                if(listener != null){
                    listener.onDrawFrame(fboText,mtx,surfaceTexture);
                }
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if(glView!= null){
            glView.requestRender();
        }
    }
    public void surfaceDestroyed(){
        if(listener != null){
            listener.onSurfaceDestroyed();
        }
        attachToGlContext = false;
    }

    @Override
    public void onUpdate(SurfaceTexture texture) {
        surfaceTexture = texture;
        surfaceTexture.setOnFrameAvailableListener(this);
//        glView.requestRender();
    }
}
