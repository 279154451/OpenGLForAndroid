package com.qi.tai.opengl.base.media.video;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;
import com.qi.tai.opengl.base.gles.utils.OpenGLUtils;
import com.qi.tai.opengl.base.media.FileRenderOutputData;
import com.qi.tai.opengl.base.media.IMediaFileCodec;
import com.qi.tai.opengl.base.media.MediaFileCodecCallBack;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class VP9VideoMediaFileCodec implements IMediaFileCodec {
    private String TAG = "VP9VideoMediaFileCodec";
    private String filePath;
    private int surfaceTextureId;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private int videoWidth = 720, videoHeight = 1280;
    private volatile boolean startPlay = false;
    private HandlerThread mVideoThread;
    private WorkHandler mVideoHandler;
    private boolean isLoop = false;
    private FileRenderOutputData renderOutputData;
    private MediaFileCodecCallBack codecCallBack;
    private volatile boolean isRelease = true;
    private boolean isBackground = false;
    private DataSource.Factory dataSourceFactory;
    private SimpleExoPlayer player;
    private MediaSource mediaSource;
    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private Context context;
    private int mSpeed = 30;
    public VP9VideoMediaFileCodec(Context context,String filePath, int speed, boolean isLoop, boolean isBackground) {
        this.filePath = filePath;
        this.isLoop = isLoop;
        this.mSpeed = speed;
        this.isBackground = isBackground;
        this.context = context.getApplicationContext();
        dataSourceFactory = VP9VideoHelper.getHelper().buildDataSourceFactory(context);
    }

    public void setLoop(boolean loop) {
        isLoop = loop;
    }

    public void setCodecCallBack(MediaFileCodecCallBack codecCallBack) {
        this.codecCallBack = codecCallBack;
    }

    private class WorkHandler extends Handler {
        public static final int REFRESH = 1;

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH:
                    if(startPlay){
                        if(codecCallBack!= null){
                            if(renderOutputData!= null){
                                codecCallBack.onFrame(renderOutputData);
                            }
                            sendEmptyMessageDelayed(REFRESH,mSpeed);
                        }else {
                            removeMessages(REFRESH);
                        }
                    }
                    break;
            }
        }
    }

    public boolean isRelease() {
        if (isRelease) {
            return true;
        }
        return false;
    }

    public boolean isPlaying() {
        return startPlay;
    }

    public void init() {
        if (isRelease()) {
            Log.d(TAG, "init isBackground="+isBackground);
            mVideoThread = new HandlerThread("VideoFile"+System.currentTimeMillis());
            mVideoThread.start();
            mVideoHandler = new WorkHandler(mVideoThread.getLooper());
            initSurface();
            initPlayer();
            isRelease = false;
        } else {
            Log.d(TAG, "has inited isBackground="+isBackground);
        }
    }


    public synchronized void start() {
        if (mVideoHandler == null) return;
        if (!isPlaying()) {
            Log.d(TAG, "start isBackground="+isBackground+" mediaSource="+mediaSource+" isRelease="+isRelease);
            startPlay = true;
            player.prepare(mediaSource);
        } else {
            Log.d(TAG, "isStarting isBackground="+isBackground);
        }
    }

    public synchronized void stopPlay() {
        if (mVideoHandler == null) return;
        Log.d(TAG, "stopPlay isBackground="+isBackground);
        startPlay = false;
        player.stop();
        mVideoHandler.removeMessages(WorkHandler.REFRESH);
        if(codecCallBack!= null){
            codecCallBack.onStop(isBackground,isRelease());
        }
    }

    public synchronized void release() {
        if(!isRelease()){
            isRelease = true;
            stopPlay();
            player.release();
            player = null;
            mediaSource = null;
            trackSelector = null;
            Log.d(TAG, "release isBackground:"+isBackground);
            if(codecCallBack != null){
                codecCallBack.onRelease(isBackground,this);
            }
            mVideoHandler.removeMessages(WorkHandler.REFRESH);
            mVideoHandler = null;
            if (mSurface != null) {
                mSurface.release();
            }
            mVideoThread.quitSafely();
        }
    }


    public int getSurfaceTextureId() {
        return surfaceTextureId;
    }

    private void initSurface() {
        surfaceTextureId = OpenGLUtils.createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mSurfaceTexture = new SurfaceTexture(surfaceTextureId);
//        mSurfaceTexture.setDefaultBufferSize(mCameraWidth, mCameraHeight);
        mSurface = new Surface(mSurfaceTexture);
    }
    private void initPlayer(){
        DrmSessionManager<ExoMediaCrypto> drmSessionManager = DrmSessionManager.getDummyDrmSessionManager();
        mediaSource = createLeafMediaSource(Uri.parse(filePath), "webm", drmSessionManager);
        TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();
        RenderersFactory renderersFactory = buildRenderersFactory(false);
        DefaultTrackSelector.ParametersBuilder builder =
                new DefaultTrackSelector.ParametersBuilder(/* context= */ context);
        trackSelectorParameters = builder.build();
        trackSelector = new DefaultTrackSelector(/* context= */ context, trackSelectionFactory);
        trackSelector.setParameters(trackSelectorParameters);
        player = new SimpleExoPlayer.Builder(/* context= */ context, renderersFactory)
                        .setTrackSelector(trackSelector)
                        .build();
        player.setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus= */ true);
        player.setPlayWhenReady(true);
        player.addAnalyticsListener(new EventLogger(trackSelector));
        player.setVideoSurface(mSurface);
//        player.prepare(mediaSource);
        player.getAnalyticsCollector().addListener(new AnalyticsListener() {
            @Override
            public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
                Log.d(TAG,"AnalyticsListener onPlayerStateChanged "+mediaSource);
                if (playbackState == Player.STATE_ENDED) {
                    if(isLoop && mediaSource!= null){
                        player.prepare(mediaSource);
                    }else {
                        mVideoHandler.removeMessages(WorkHandler.REFRESH);
                        if(codecCallBack!= null){
                            codecCallBack.onStop(isBackground,isRelease());
                        }
                    }
                }else if(playbackState == Player.STATE_READY){

                }
            }

            @Override
            public void onIsPlayingChanged(EventTime eventTime, boolean isPlaying) {
                Log.d(TAG,"AnalyticsListener onIsPlayingChanged "+isPlaying);
            }

            @Override
            public void onTimelineChanged(EventTime eventTime, int reason) {
                Log.d(TAG,"AnalyticsListener onTimelineChanged "+eventTime.realtimeMs);
            }

            @Override
            public void onSeekProcessed(EventTime eventTime) {
                Log.d(TAG,"AnalyticsListener onSeekProcessed");
            }

            @Override
            public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {
                Log.d(TAG,"AnalyticsListener onPlaybackParametersChanged");
            }

            @Override
            public void onLoadCompleted(EventTime eventTime, MediaSourceEventListener.LoadEventInfo loadEventInfo, MediaSourceEventListener.MediaLoadData mediaLoadData) {
                Log.d(TAG,"AnalyticsListener onLoadCompleted");
            }

            @Override
            public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
                Log.d(TAG,"AnalyticsListener onSurfaceSizeChanged "+width+" "+height);
            }

            @Override
            public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
                Log.d(TAG,"AnalyticsListener onDecoderInputFormatChanged");
            }

            @Override
            public void onDroppedVideoFrames(EventTime eventTime, int droppedFrames, long elapsedMs) {
                Log.d(TAG,"AnalyticsListener onDroppedVideoFrames");
            }

            @Override
            public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                Log.d(TAG,"AnalyticsListener onVideoSizeChanged "+width+" "+height);
                videoWidth = width;
                videoHeight = height;
            }

            @Override
            public void onRenderedFirstFrame(EventTime eventTime, @Nullable @org.jetbrains.annotations.Nullable Surface surface) {
                Log.d(TAG,"AnalyticsListener onRenderedFirstFrame "+eventTime.realtimeMs+" "+surface);
                renderOutputData = new FileRenderOutputData(videoWidth,videoHeight,surfaceTextureId,isBackground,0,mSurfaceTexture);
                if(startPlay && mVideoThread.isAlive()){
                    mVideoHandler.removeMessages(WorkHandler.REFRESH);
                    mVideoHandler.sendEmptyMessage(WorkHandler.REFRESH);
                }
            }
        });
    }

    public RenderersFactory buildRenderersFactory(boolean preferExtensionRenderer) {
        int extensionRendererMode =DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER;
        return new DefaultRenderersFactory(/* context= */ context).setExtensionRendererMode(extensionRendererMode);
    }
    private MediaSource createLeafMediaSource(
            Uri uri, String extension, DrmSessionManager<?> drmSessionManager) {
        @C.ContentType int type = Util.inferContentType(uri, extension);
        switch (type) {
            case C.TYPE_OTHER:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            default:
                throw new IllegalStateException("Unsupported type: " + type);
        }
    }
}
