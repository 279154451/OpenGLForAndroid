package com.single.code.android.opengl1.record;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.util.Log;

import com.single.code.android.opengl1.render.IRenderListener;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RecordManager implements IRenderListener ,RecordStatusCallBack{
    private volatile static RecordManager manager;
    private Speed mSpeed = Speed.MODE_NORMAL;
    public static RecordManager getManager(){
        if(manager == null){
            synchronized (RecordManager.class){
                if(manager == null){
                    manager = new RecordManager();
                }
            }
        }
        return manager;
    }
    MediaRecorder mediaRecorder;
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mediaRecorder = new MediaRecorder(EGL14.eglGetCurrentContext());
        mediaRecorder.setStatusCallBack(this);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if(mediaRecorder != null){
//            String filePath = ContextCompat.getExternalFilesDirs(surface.getContext(), Environment.DIRECTORY_DCIM)[0].getAbsolutePath()+ File.separator+"a.mp4";
            mediaRecorder.surfaceChange(width,height);
        }
    }

    @Override
    public void onDrawFrame(int textureId, float[] mtx, SurfaceTexture surfaceTexture) {
        mediaRecorder.fireFrame(textureId,mtx,surfaceTexture.getTimestamp());
    }

    @Override
    public void onSurfaceDestroyed() {

    }

    public void startRecord(String filePath){
        //速度  时间/速度 speed小于就是放慢 大于1就是加快
        float speed = 1.f;
        switch (mSpeed) {
            case MODE_EXTRA_SLOW:
                speed = 0.3f;
                break;
            case MODE_SLOW:
                speed = 0.5f;
                break;
            case MODE_NORMAL:
                speed = 1.f;
                break;
            case MODE_FAST:
                speed = 2.f;
                break;
            case MODE_EXTRA_FAST:
                speed = 3.f;
                break;
        }
        mediaRecorder.startRecord(speed,filePath);
    }
    public void setSpeed(Speed speed){
        mSpeed = speed;
    }
    public enum Speed {
        MODE_EXTRA_SLOW, MODE_SLOW, MODE_NORMAL, MODE_FAST, MODE_EXTRA_FAST
    }
    public void stopRecord(){
        mediaRecorder.stopRecord();
    }
    private String TAG = RecordManager.class.getSimpleName();

    @Override
    public void onStart() {
        Log.d(TAG, "onStart: ");
    }

    @Override
    public void onComplete(String filePath) {
        Log.d(TAG, "onComplete: "+filePath);

    }

    @Override
    public void onError() {
        Log.d(TAG, "onError: ");
    }
}
