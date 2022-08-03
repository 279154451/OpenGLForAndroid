package com.qi.tai.opengl.base.media.video;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;


import com.qi.tai.opengl.base.gles.utils.OpenGLUtils;
import com.qi.tai.opengl.base.media.FileRenderOutputData;
import com.qi.tai.opengl.base.media.IMediaFileCodec;
import com.qi.tai.opengl.base.media.MediaFileCodecCallBack;
import com.qi.tai.opengl.base.media.video.provider.VideoFileProvider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


public class VideoMediaFileCodec implements IMediaFileCodec {
    private String TAG = "MediaVideoFilePlayer";
    private MediaExtractor mMediaExtractor;
    private String filePath;
    private MediaCodec mediaCodec;
    private int trackCount;
    private int surfaceTextureId;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private int videoWidth = 720, videoHeight = 1280;
    private MediaFormat mediaFormat;
    private volatile boolean startCodec = false;
    private MediaCodec.BufferInfo mBufferInfo;
    private HandlerThread mVideoThread;
    private WorkHandler mVideoHandler;
    private boolean isLoop = false;
    private FileRenderOutputData renderOutputData;
    private MediaFileCodecCallBack codecCallBack;
    private volatile boolean isRelease = true;
    private int maxInputSize = 0;
    private String mime;
    private boolean isBackground = false;
    private long speed = 0;
    private boolean needFrameData;//是否需要获取每一帧的数据
    private boolean asynCodec = true;//是否异步解码
    protected ReentrantLock codecLock = new ReentrantLock(true);
    public final static int[] DEFAULT_SUPPORT_FORMAT = new int[]{
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
    };

    /**
     * Track Number
     */
    protected int mTrackIndex;
    private VideoFileProvider fileProvider;

    public VideoMediaFileCodec(VideoFileProvider fileProvider, boolean needFrameData,boolean asynCodec, boolean isLoop, long speed, boolean isBackground) {
        this.fileProvider = fileProvider;
        this.filePath = fileProvider.getFilePath();
        this.isLoop = isLoop;
        this.speed = speed;
        this.isBackground = isBackground;
        this.needFrameData = needFrameData;
        this.asynCodec = asynCodec;
    }

    public void setCodecCallBack(MediaFileCodecCallBack codecCallBack) {
        this.codecCallBack = codecCallBack;
    }

    private class WorkHandler extends Handler {
        public static final int CODEC = 0;

        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CODEC:
                    if(!asynCodec){
                        onPlay();
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

    public boolean isCodec() {
        return startCodec;
    }


    public void init() {
        codecLock.lock();
        try {
            if (isRelease()) {
                isRelease = false;
                mVideoThread = new HandlerThread("VideoFile" + System.currentTimeMillis()+filePath);
                mVideoThread.start();
                mVideoHandler = new WorkHandler(mVideoThread.getLooper());
                initSurface();
                initMediaExtractor();
                initDecoder();
            } else {
                Log.d(TAG, "has inited isBackground=" + isBackground);
            }
        }finally {
            codecLock.unlock();
        }
    }

    private void initMediaExtractor() {
        try {
            mTrackIndex = 0;
            // 创建解封装器
            mMediaExtractor = fileProvider.getMediaExtractor();
            // 遍历所有轨道
            trackCount = mMediaExtractor.getTrackCount();
            Log.d(TAG, "initMediaExtractor getTrackCount: " + trackCount + " filePath:" + filePath);
            for (int i = 0; i < trackCount; ++i) {
                mediaFormat = mMediaExtractor.getTrackFormat(i);
                String KEY_MIME = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (KEY_MIME.contains("video/")) {
                    mTrackIndex = i;
                    mMediaExtractor.selectTrack(i);
                    if (!filePath.endsWith(".webm")) {
                        maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    }
                    mime = KEY_MIME;
                    videoWidth = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
                    videoHeight = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    int rotation = 0;
                    if (mediaFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                        //  视频旋转顺时针角度
                        rotation = mediaFormat.getInteger(MediaFormat.KEY_ROTATION);
                    }
                    Log.d(TAG, "initMediaExtractor mime=" + mime + " videoWidth=" + videoWidth + " videoHeight=" + videoHeight);
                    renderOutputData = new FileRenderOutputData(videoWidth, videoHeight, surfaceTextureId, isBackground, rotation, mSurfaceTexture);
                    renderOutputData.setFilePath(filePath);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (mVideoHandler != null) {
            codecLock.lock();
            try {
                if (!isCodec() && !isRelease()) {
                    Log.d(TAG, "startPlay isBackground=" + isBackground + " filepath:" + filePath);
                    if(mVideoThread.isAlive() && mVideoHandler!= null){
                        startCodec = true;
                        mVideoHandler.removeMessages(WorkHandler.CODEC);
                        mVideoHandler.sendEmptyMessage(WorkHandler.CODEC);
                    }
                } else {
                    Log.d(TAG, "isPlaying isBackground=" + isBackground);
                }
            }finally {
                codecLock.unlock();
            }
        }
    }
    public void release() {
        codecLock.lock();
        try {
            Log.d(TAG, "release isRelease:" + isRelease + " filePath " + filePath);
            boolean hasInit = !isRelease();
            isRelease = true;
            startCodec = false;
            if (hasInit) {
                Log.d(TAG, "release isBackground:" + isBackground + " filePath=" + filePath);
                if (codecCallBack != null) {
                    codecCallBack.onRelease(isBackground,this);
                }
                if(mVideoThread.isAlive())mVideoHandler.removeMessages(WorkHandler.CODEC);
                if (mSurface != null) {
                    mSurface.release();
                }
                if (mMediaExtractor != null) {
                    mMediaExtractor.release();
                }
                if (mediaCodec != null) {
                    mediaCodec.release();
                }
                mVideoThread.quitSafely();
                mVideoHandler = null;
            }
            codecCallBack = null;
        }finally {
            codecLock.unlock();
        }
    }

    private void onPlay() {
        Log.d(TAG, "onPlay " + isCodec() + " " + isRelease());
        mediaCodec.flush();
        if(codecCallBack!= null && !isRelease()){
            codecCallBack.onStart(filePath,false);
        }
        while (isCodec()) {
            codecLock.lock();
            try {
                if (isRelease()) return;
                int inputBufferId = mediaCodec.dequeueInputBuffer(0);
                if (inputBufferId > 0 ) {
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferId);
                    final ReadBufferResult readBufferResult = readBufferFromMediaExtractor(inputBuffer);
                    if (readBufferResult.size < 0 ) {
                        mediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        if (isLoop) {
                            toSeek(0);
                        } else {
                            startCodec = false;
                        }
                    } else {
                        mediaCodec.queueInputBuffer(inputBufferId, 0, readBufferResult.size, readBufferResult.presentationTimeUs, 0);
                    }
                }
                while (isCodec()) {
                    int outputBufferId = mediaCodec.dequeueOutputBuffer(mBufferInfo, 0);
                    if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break;
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        continue;
                    } else if (outputBufferId > 0) {
                        // 获取到处理完成的数据和格式
                        if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                            mediaCodec.releaseOutputBuffer(outputBufferId, false);
                            break;
                        }
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferId);
                        // 封装或渲染
                        renderOutputData.setOutputBuffer(outputBuffer);
                        if (needFrameData) {
                            Image image = mediaCodec.getOutputImage(outputBufferId);
                            byte[] bytes = getBytesFromImageAsType(image, 2);
//                                compressToJpeg("/storage/emulated/0/Android/data/com.faceunity.app_ptag/files/test/download/"+System.currentTimeMillis()+".jpeg",image,bytes);
                            renderOutputData.setFrameBytes(bytes);
                        }
                        if(codecCallBack != null)codecCallBack.onFrame(renderOutputData);
                        // 释放该buffer
                        try {
                            Thread.sleep(speed);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        mediaCodec.releaseOutputBuffer(outputBufferId, true);
                    }
                }
            }finally {
                codecLock.unlock();
            }
        }
        if (codecCallBack != null) {
            codecCallBack.onStop(isBackground, isRelease);
        }
    }

    private void toSeek(long time) {
        if (mMediaExtractor != null && mediaCodec != null) {
            mediaCodec.flush();
            mMediaExtractor.seekTo(time, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            if(codecCallBack!= null){
                codecCallBack.onStart(filePath,true);
            }
        }
    }

    public static class ReadBufferResult {
        public int size;
        public long presentationTimeUs;
        public int maxInputSize;

        public ReadBufferResult(int size, long presentationTimeUs, int maxInputSize) {
            this.size = size;
            this.presentationTimeUs = presentationTimeUs;
            this.maxInputSize = maxInputSize;
        }

        @Override
        public String toString() {
            return "ReadBufferResult{" +
                    "size=" + size +
                    ", presentationTimeUs=" + presentationTimeUs +
                    ", maxInputSize=" + maxInputSize +
                    '}';
        }
    }


    /**
     * 从解封器中读取数据
     *
     * @return
     */
    private ReadBufferResult readBufferFromMediaExtractor(ByteBuffer mInputBuffer) {
//        mInputBuffer = ByteBuffer.allocate(maxInputSize);
//        mMediaExtractor.selectTrack(mTrackIndex);
        if (mInputBuffer == null) {
            return new ReadBufferResult(-1, 0, maxInputSize);
        } else {
            int size = mMediaExtractor.readSampleData(mInputBuffer, 0);
            long presentationTimeUs = mMediaExtractor.getSampleTime();
            mMediaExtractor.advance();  // 下一个样本
            return new ReadBufferResult(size, presentationTimeUs, maxInputSize);
        }
    }

    /**
     * 初始化解码器
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void initDecoder() {
        try {
            if (TextUtils.isEmpty(mime)) {
                if (filePath.endsWith(".webm")) {
                    mime = "video/x-vnd.on2.vp9";
                } else {
                    mime = "video/avc";
                }
            }

            mediaCodec = MediaCodec.createDecoderByType(mime);
            if(asynCodec){
                mediaCodec.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int inputBufferId) {
                        // InputBufferQueue有空闲buffer回调
                        codecLock.lock();
                        try {
                            if (inputBufferId > 0) {
                                ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferId);
//                        // 从文件解封器中读取，ByteBuffer
                                final ReadBufferResult readBufferResult = readBufferFromMediaExtractor(inputBuffer);
                                if (readBufferResult.size < 0) {
                                    if(isLoop){
                                        codec.queueInputBuffer(inputBufferId, 0, 0, 0L, 0);
                                        mMediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                                    }
                                } else {
                                    codec.queueInputBuffer(inputBufferId, 0, readBufferResult.size, readBufferResult.presentationTimeUs, 0);
                                }
                            }
                        }finally {
                            codecLock.unlock();
                        }
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int outputBufferId, @NonNull MediaCodec.BufferInfo info) {
                       codecLock.lock();
                       try {
                           if (outputBufferId > 0) {
                               // 获取到处理完成的数据和格式
                               if (info.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                   codec.releaseOutputBuffer(outputBufferId, false);
                               }else {
                                   ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
                                   // 封装或渲染
                                   renderOutputData.setOutputBuffer(outputBuffer);
                                   if (needFrameData) {
                                       Image image = codec.getOutputImage(outputBufferId);
                                       byte[] bytes = getBytesFromImageAsType(image, 2);
                                       renderOutputData.setFrameBytes(bytes);
                                   }
                                   if(codecCallBack != null)codecCallBack.onFrame(renderOutputData);
                                   // 释放该buffer
                                   try {
                                       Thread.sleep(speed);
                                   } catch (InterruptedException e) {
                                       e.printStackTrace();
                                   }
                                   codec.releaseOutputBuffer(outputBufferId, true);
                               }
                           }
                       }finally {
                           codecLock.unlock();
                       }
                    }

                    @Override
                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                        Log.d(TAG,"onError ");
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                        Log.d(TAG,"onOutputFormatChanged "+format.toString());
                    }
                },mVideoHandler);
            }
            mBufferInfo = new MediaCodec.BufferInfo();
//            mediaFormat = MediaFormat.createVideoFormat(mime, videoWidth, videoWidth);
// 设置color_format，如果使用的是InputSurface，则设置为FormatSurface
// 码率
//            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
//// 帧率
//            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
//// gop
//            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
//
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatRGBAFlexible);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_STANDARD,MediaFormat.COLOR_STANDARD_BT709);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_RANGE,MediaFormat.COLOR_RANGE_FULL);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_TRANSFER,MediaFormat.COLOR_TRANSFER_LINEAR);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (mSurfaceTexture.isReleased()) {
                    initSurface();
                }
            }
//            selectedColorFormat(mediaCodec,mime);
            if (needFrameData) {
                mediaCodec.configure(mediaFormat, null, null, 0);
            } else {
                mediaCodec.configure(mediaFormat, mSurface, null, 0);
            }
            if (!isRelease()) {
                mediaCodec.start();
                Log.i(TAG, "initDecoder mediaCodec.start");
            }else {
                Log.i(TAG, "initDecoder has released");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectedColorFormat(MediaCodec decoder, String mime) {
        int colorFormatIndex = getColorFormats(decoder, mime);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, DEFAULT_SUPPORT_FORMAT[colorFormatIndex]);
        Log.i(TAG, "setDecoderParams: " + "set decode color format to type " + Integer.toHexString(DEFAULT_SUPPORT_FORMAT[colorFormatIndex]));
    }


    public int getColorFormats(MediaCodec codec, String mime) {
        try {
            MediaCodecInfo codecInfo = codec.getCodecInfo();
            MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mime);
            Log.i(TAG, "getColorFormats " + Arrays.toString(capabilities.colorFormats));
            for (int i = 0; i < DEFAULT_SUPPORT_FORMAT.length; i++) {
                for (int f : capabilities.colorFormats) {
                    if (DEFAULT_SUPPORT_FORMAT[i] == f)
                        return i;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        System.out.print("supported color format: ");
        for (int c : caps.colorFormats) {
            System.out.print(c + "\t");
        }
        System.out.println();
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

    //根据image获取yuv值-------------------NEW
    public static byte[] getBytesFromImageAsType(Image image, int type) {
        try {
            //获取源数据，如果是YUV格式的数据planes.length = 3
            //plane[i]里面的实际数据可能存在byte[].length <= capacity (缓冲区总大小)
            final Image.Plane[] planes = image.getPlanes();

            //数据有效宽度，一般的，图片width <= rowStride，这也是导致byte[].length <= capacity的原因
            // 所以我们只取width部分
            int width = image.getWidth();
            int height = image.getHeight();

            //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1 （这里是YUV_420_888）
            byte[] yuvBytes = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            //目标数组的装填到的位置
            int dstIndex = 0;

            //临时存储uv数据的
            byte uBytes[] = new byte[width * height / 4];
            byte vBytes[] = new byte[width * height / 4];
            int uIndex = 0;
            int vIndex = 0;

            int pixelsStride, rowStride;
            for (int i = 0; i < planes.length; i++) {
                pixelsStride = planes[i].getPixelStride();
                rowStride = planes[i].getRowStride();

                ByteBuffer buffer = planes[i].getBuffer();

                //如果pixelsStride==2，一般的Y的buffer长度=640*480，UV的长度=640*480/2-1
                //源数据的索引，y的数据是byte中连续的，u的数据是v向左移以为生成的，两者都是偶数位为有效数据
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                int srcIndex = 0;
                if (i == 0) {
                    //直接取出来所有Y的有效区域，也可以存储成一个临时的bytes，到下一步再copy
                    for (int j = 0; j < height; j++) {
                        System.arraycopy(bytes, srcIndex, yuvBytes, dstIndex, width);
                        srcIndex += rowStride;
                        dstIndex += width;
                    }
                } else if (i == 1) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            uBytes[uIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                } else if (i == 2) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            vBytes[vIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                }
            }
            //   image.close();
            //根据要求的结果类型进行填充
            switch (type) {
                case 0://YUV420P
                    System.arraycopy(uBytes, 0, yuvBytes, dstIndex, uBytes.length);
                    System.arraycopy(vBytes, 0, yuvBytes, dstIndex + uBytes.length, vBytes.length);
                    break;
                case 1://YUV420SP
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = uBytes[i];
                        yuvBytes[dstIndex++] = vBytes[i];
                    }
                    break;
                case 2://NV21
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = vBytes[i];
                        yuvBytes[dstIndex++] = uBytes[i];
                    }
                    break;
            }
            return yuvBytes;
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
        }
        return null;
    }

    /**
     * 保存为图片
     *
     * @param fileName
     * @param image
     * @param bytes
     */
    private void compressToJpeg(String fileName, Image image, byte[] bytes) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create output file " + fileName, ioe);
        }
        Rect rect = image.getCropRect();
        YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, rect.width(), rect.height(), null);
        yuvImage.compressToJpeg(rect, 100, outStream);
    }
}
