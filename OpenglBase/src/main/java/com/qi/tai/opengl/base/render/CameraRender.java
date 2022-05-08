package com.qi.tai.opengl.base.render;

import android.graphics.SurfaceTexture;
import android.opengl.EGLConfig;


import com.qi.tai.opengl.base.camera.IPreviewOutputUpdateListener;
import com.qi.tai.opengl.base.gles.BaseProgram;
import com.qi.tai.opengl.base.gles.program.CameraFBOProgram;
import com.qi.tai.opengl.base.gles.program.ScreenProgram;
import com.qi.tai.opengl.base.record.RecordManager;
import com.qi.tai.opengl.base.surface.GLView;


/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraRender extends BaseRender implements  IPreviewOutputUpdateListener,SurfaceTexture.OnFrameAvailableListener {
    private int[] textureId;
    private SurfaceTexture surfaceTexture;
    private BaseProgram cameraProgram;//fbo着色器程序
    private BaseProgram screenProgram;//屏幕着色器程序
    private IRenderStatusListener listener;
    public CameraRender(GLView glView){
        super(glView);
        textureId = new int[1];
        listener = RecordManager.getManager();
    }
    private volatile boolean attachToGlContext =false;

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        cameraProgram = new CameraFBOProgram();
        screenProgram = new ScreenProgram();
        if(listener != null){
            listener.onSurfaceCreated(config);
        }
    }
    @Override
    public void onSurfaceChanged(int width, int height) {
        cameraProgram.surfaceChange(width,height);
        screenProgram.surfaceChange(width,height);
        if(listener != null){
            listener.onSurfaceChanged(width,height);
        }
    }

    @Override
    public void onSurfaceDestroy() {
        super.onSurfaceDestroy();
        if(listener != null){
            listener.onSurfaceDestroyed();
        }
        attachToGlContext = false;
    }

    float[] mtx = new float[16];
    @Override
    public void onDrawFrame() {
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

    @Override
    public void onUpdate(SurfaceTexture texture) {
        surfaceTexture = texture;
        surfaceTexture.setOnFrameAvailableListener(this);
//        glView.requestRender();
    }
}
