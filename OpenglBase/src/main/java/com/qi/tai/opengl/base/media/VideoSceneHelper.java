package com.qi.tai.opengl.base.media;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;


import com.qi.tai.opengl.base.media.video.VideoMediaFileCodec;
import com.qi.tai.opengl.base.media.video.provider.AssetsFileProvider;
import com.qi.tai.opengl.base.media.video.provider.RawFileProvider;

import java.util.concurrent.ConcurrentLinkedQueue;

public class VideoSceneHelper implements MediaFileCodecCallBack {
    private String TAG = "VideoSceneHelper";
    private static volatile VideoSceneHelper helper;
    private MediaFileCodecCallBack codecCallBack;
    private ConcurrentLinkedQueue<IMediaFileCodec> waitPlayQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<IMediaFileCodec> waitReleaseQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<FileRenderOutputData> waitUpdateQueue = new ConcurrentLinkedQueue<>();
    private IMediaFileCodec videoCodec;
    private boolean isAvatarLoaded = false;
    private String currentPlayFile;
    private FileRenderOutputData currentOutputData;
    private Context context;
    private Handler handler = new Handler(Looper.getMainLooper());

    public void initCodec(Context context, MediaFileCodecCallBack codecCallBack) {
        Log.d(TAG, "initCodec: ");
        this.context = context.getApplicationContext();
        this.codecCallBack = codecCallBack;
    }

    public static VideoSceneHelper getHelper() {
        if (helper == null) {
            synchronized (VideoSceneHelper.class) {
                if (helper == null) {
                    helper = new VideoSceneHelper();
                }
            }
        }
        return helper;
    }

    public void setAvatarLoaded(boolean avatarLoaded) {
        Log.d(TAG, "setAvatarLoaded: " + avatarLoaded);
        isAvatarLoaded = avatarLoaded;
    }
    public boolean playVideo(final String filePath){
        Log.d(TAG, "playVideo: " + filePath+" "+currentPlayFile+" "+videoCodec);
        if(TextUtils.equals(currentPlayFile,filePath)){
            Log.d(TAG, "playVideo: is same file");
            return false;
        }
        currentPlayFile = filePath;
        waitPlayQueue.clear();
        waitReleaseQueue.clear();
        waitUpdateQueue.clear();
        if(videoCodec!= null){
            videoCodec.release();
        }
        if(filePath.startsWith("android.resource://")){
            videoCodec = new VideoMediaFileCodec(new RawFileProvider(context,filePath),true,false,true,30,true);
        }else {
            videoCodec = new VideoMediaFileCodec(new AssetsFileProvider(context,filePath),true,false,true,30,true);
        }
        startVideo(videoCodec);
        return true;
    }

    public void startVideo(IMediaFileCodec videoCodec) {
        Log.d(TAG, "startVideo: " + videoCodec+" "+currentPlayFile);
        if(videoCodec!= null){
            currentOutputData = null;
            waitPlayQueue.offer(videoCodec);
        }
    }

    public void onPause() {
        Log.d(TAG, "onPause: ");
        waitUpdateQueue.clear();
        if(videoCodec!= null){
            waitReleaseQueue.offer(videoCodec);
        }
    }

    public void onResume() {
        Log.d(TAG, "onResume: ");
        waitUpdateQueue.clear();
        startVideo(videoCodec);
    }

    public void onRenderBefore() {
        while (!waitReleaseQueue.isEmpty()) {
            final IMediaFileCodec videoMediaFileCodec = waitReleaseQueue.poll();
            if (videoMediaFileCodec != null && !videoMediaFileCodec.isRelease()) {
                videoMediaFileCodec.release();
            }
        }
        if (isAvatarLoaded) {
            while (!waitPlayQueue.isEmpty()) {
                final IMediaFileCodec videoMediaFileCodec = waitPlayQueue.poll();
                if (videoMediaFileCodec != null) {
                    if (videoMediaFileCodec.isRelease()) {
                        videoMediaFileCodec.init();
                    }
                    if(!videoMediaFileCodec.isRelease()){
                        videoMediaFileCodec.start();
                        videoMediaFileCodec.setCodecCallBack(this);
                    }
                }
            }
        }
        while (!waitUpdateQueue.isEmpty()) {
            FileRenderOutputData output = waitUpdateQueue.poll();
            if (output != null) {
                output.updateTexImage();
            }
        }
    }

    public void onRenderAfter() {

    }

    public void release() {
        Log.d(TAG, "release: ");
        waitPlayQueue.clear();
        waitReleaseQueue.clear();
        waitUpdateQueue.clear();
        if (videoCodec != null) {
            videoCodec.release();
            videoCodec = null;
        }
        currentPlayFile= null;
        isAvatarLoaded = false;
        codecCallBack = null;
        currentOutputData = null;
    }

    private boolean isSameOutPut(FileRenderOutputData outputData) {
        if (currentOutputData != null) {
            if (currentOutputData.getWidth() == outputData.getWidth() && currentOutputData.getHeight() == outputData.getHeight() && currentOutputData.isBackground() == outputData.isBackground() && currentOutputData.getTextId() == outputData.getTextId()) {
                return true;
            } else {
                currentOutputData = outputData;
                currentOutputData.setPlaying(true);
                return false;
            }
        } else {
            currentOutputData = outputData;
            currentOutputData.setPlaying(true);
            return false;
        }
    }

    @Override
    public void onFrame(FileRenderOutputData outputData) {
//        Log.d(TAG, "onFrame: "+outputData);
        waitUpdateQueue.offer(outputData);
        if (codecCallBack != null) {
            codecCallBack.onFrame(outputData);
        }
//        if (!isSameOutPut(outputData)) {
//            if (codecCallBack != null) {
//                codecCallBack.onFrame(outputData);
//            }
//        }
    }

    @Override
    public void onStop(boolean isBackground, boolean isRelease) {
        Log.d(TAG, "onStop: " + isBackground + " " + isRelease);
        if (codecCallBack != null) {
            codecCallBack.onStop(isBackground, isRelease);
        }
        if (!waitUpdateQueue.isEmpty()) {
            final FileRenderOutputData peek = waitUpdateQueue.peek();
            if (peek != null) {
                if (peek.isBackground() == isBackground) {
                    waitUpdateQueue.remove(peek);
                }
            }
        }
        if (currentOutputData != null && currentOutputData.isBackground() == isBackground) {
            currentOutputData.setPlaying(false);
        }
    }

    @Override
    public void onRelease(boolean isBackground,IMediaFileCodec mediaFileCodec) {
        Log.d(TAG, "onRelease: " + isBackground);
        if (codecCallBack != null) {
            codecCallBack.onRelease(isBackground,mediaFileCodec);
        }
        if (!waitUpdateQueue.isEmpty()) {
            final FileRenderOutputData peek = waitUpdateQueue.peek();
            if (peek != null) {
                if (peek.isBackground() == isBackground) {
                    waitUpdateQueue.remove(peek);
                }
            }
        }
    }

    @Override
    public void onStart(String filePath,boolean restart) {
        Log.d(TAG, "onStart: " + filePath+" restart="+restart);
        if (codecCallBack != null) {
            codecCallBack.onStart(filePath,restart);
        }
    }
}
