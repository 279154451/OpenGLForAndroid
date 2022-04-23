package com.single.code.android.opengl1.record;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.opengl.EGLContext;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MediaRecorder {
    private String TAG = MediaRecorder.class.getSimpleName();
    private String filePath;
    private EGLContext glContext;//GL线程的上下文，用来与录制线程的GL上下文共享纹理数据，只有这样录制线程才能拿到处理完之后显示的图像纹理。
    private int width;
    private int height;
    private EGLEnv recordGLEnv;//录制线程GL环境
    private MediaCodec mMediaCodec;
    private Surface mSurface;
    private MediaMuxer mMuxer;
    private Handler recordHandler;
    private volatile boolean glReady = false;
    private float mSpeed;
    private int track;
    private long mLastTimeStamp;
    private RecordStatusCallBack statusCallBack;
    public MediaRecorder(EGLContext glContext) {
        this.glContext = glContext;
    }

    public void setStatusCallBack(RecordStatusCallBack statusCallBack) {
        this.statusCallBack = statusCallBack;
    }

    public void surfaceChange(int width, int height){
        this.width = width;
        this.height =height;
    }

    private void initMediaCodec() {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        //颜色空间 从 surface当中获得
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
                .COLOR_FormatSurface);
        //码率
        format.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000);
        //帧率
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
        //关键帧间隔
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10);
        //创建编码器
        try {
            mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //配置编码器
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //这个surface显示的内容就是要编码的画面
        mSurface = mMediaCodec.createInputSurface();

        //混合器 (复用器) 将编码的h.264封装为mp4
        try {
            mMuxer = new MediaMuxer(filePath,
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //开启编码
        mMediaCodec.start();


        //創建录制的OpenGL线程环境
        HandlerThread handlerThread = new HandlerThread("codec-gl");
        handlerThread.start();
        recordHandler = new Handler(handlerThread.getLooper());
        recordHandler.post(new Runnable() {
            @Override
            public void run() {
                // 创建EGL环境
                recordGLEnv = new EGLEnv(glContext, mSurface, width, height);
                glReady = true;
                onStart();
            }
        });
    }
    //编码
    private void codec(boolean endOfStream) {
        long codecStartTime = System.currentTimeMillis();
        //给个结束信号
        if (endOfStream) {
            mMediaCodec.signalEndOfInputStream();
        }
        while (true) {
            //获得输出缓冲区 (编码后的数据从输出缓冲区获得)
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10_000);
            //需要更多数据
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                //如果是结束那直接退出，否则继续循环
                if (!endOfStream) {
                    onError(-1);
                    break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                //输出格式发生改变  第一次总会调用所以在这里开启混合器
                MediaFormat newFormat = mMediaCodec.getOutputFormat();
                track = mMuxer.addTrack(newFormat);
                mMuxer.start();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                //可以忽略
            } else {
                //调整时间戳
                bufferInfo.presentationTimeUs = (long) (bufferInfo.presentationTimeUs / mSpeed);
                //有时候会出现异常 ： timestampUs xxx < lastTimestampUs yyy for Video track
                if (bufferInfo.presentationTimeUs <= mLastTimeStamp) {
                    bufferInfo.presentationTimeUs = (long) (mLastTimeStamp + 1_000_000 / 25 / mSpeed);
                }
                mLastTimeStamp = bufferInfo.presentationTimeUs;

                //正常则 encoderStatus 获得缓冲区下标
                ByteBuffer encodedData = mMediaCodec.getOutputBuffer(encoderStatus);
                //如果当前的buffer是配置信息，不管它 不用写出去
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    //设置从哪里开始读数据(读出来就是编码后的数据)
                    encodedData.position(bufferInfo.offset);
                    //设置能读数据的总长度
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    //写出为mp4
                    mMuxer.writeSampleData(track, encodedData, bufferInfo);
                }
                // 释放这个缓冲区，后续可以存放新的编码后的数据啦
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                // 如果给了结束信号 signalEndOfInputStream
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    onComplete(filePath);
                    break;
                }
            }
        }
        long codecTime = System.currentTimeMillis()-codecStartTime;
        Log.d(TAG, "codec: time="+codecTime);
    }
    public void fireFrame(final int textureId,float[] mtx, final long timestamp) {
        if (!glReady) {//录制的GL环境是否创建完成
            return;
        }
        //录制用的opengl已经和handler的线程绑定了 ，所以需要在这个线程中使用录制的opengl
        recordHandler.post(new Runnable() {
            @Override
            public void run() {
                //画画,绘制到mediaCodec的mSurface上
                recordGLEnv.onDraw(textureId,mtx,timestamp);
                codec(false);
            }
        });
    }

    //开始录制
    public void startRecord(float speed,String filePath) {
        this.filePath = filePath;
        this.mSpeed = speed;
        initMediaCodec();
    }

    //停止录制
    public void stopRecord() {
        glReady = false;
        recordHandler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    codec(true);
                    mMediaCodec.stop();
                    mMediaCodec.release();
                    mMediaCodec = null;
                    mMuxer.stop();
                    mMuxer.release();
                    recordGLEnv.release();
                    recordGLEnv = null;
                    mMuxer = null;
                    mSurface = null;
                    recordHandler.getLooper().quitSafely();
                    recordHandler = null;
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }
    public void release(){
        statusCallBack = null;
        stopRecord();
    }
    private void onComplete(String filePath){
        if(statusCallBack != null){
            statusCallBack.onComplete(filePath);
        }
    }
    private void onStart(){
        if(statusCallBack != null){
            statusCallBack.onStart();
        }
    }
    private void onError(int code){
        if(statusCallBack != null){
            statusCallBack.onError();
        }
    }

}
