package com.qi.tai.opengl.base.camera;

import android.graphics.ImageFormat;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;

import com.qi.tai.opengl.base.surface.GLView;

import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;


import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraXHelper extends BaseCamera implements Preview.OnPreviewOutputUpdateListener{
    private String TAG = CameraXHelper.class.getSimpleName();
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private ReentrantLock lock;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    public CameraXHelper(GLView glView, IPreviewOutputUpdateListener listener) {
        super(glView,listener);
        lock = new ReentrantLock();
    }

    @Override
    public void startPreview(int width,int height){
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        preview = getPreView();

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setCallbackHandler(new Handler(handlerThread.getLooper()))
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .setTargetAspectRatio(new Rational(2, 3))
                .setTargetResolution(new Size(width, height))
                .build();

        imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        MyAnalyzer myAnalyzer = new MyAnalyzer();
        imageAnalysis.setAnalyzer(myAnalyzer);


        CameraX.bindToLifecycle(glView.getLifecycleOwner(), preview, imageAnalysis);
    }
    private Preview getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                .setLensFacing(currentFacing) //前置或者后置摄像头
                .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(this);
        return preview;
    }

    @Override
    public void onUpdated(Preview.PreviewOutput output) {
        if(listener != null){
            listener.onUpdate(output.getSurfaceTexture());
        }
    }

    private class MyAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(ImageProxy imageProxy, int rotationDegrees) {
            final Image image = imageProxy.getImage();
           //图像格式
            int format = image.getFormat();
            Log.i(TAG, "analyze: format:" + format);
            if (format == ImageFormat.YUV_420_888) {
//                Log.i(TAG, "analyze: format:" + format);
                lock.lock();
                byte[] bytes = yuvToNv21(image);
                if(bytes!= null){
                    Log.d(TAG, "analyze: "+bytes.length);
                }
                lock.unlock();
            }
        }
    }
    //cameraX会生成YUV_420_888格式数据：https://gitee.com/kusebingtang/my-blog-markdown/blob/master/Android%20CameraX%E8%8E%B7%E5%8F%96H264%E7%A0%81%E6%B5%81.md
    public byte[] yuvToNv21(Image image ){
        byte[] mCameraNV21Byte = null;
        if (image == null) {
            return mCameraNV21Byte;
        }
        Image.Plane[] planes = image.getPlanes();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int yRowStride = planes[0].getRowStride();
        int uRowStride = planes[1].getRowStride();
        int vRowStride = planes[2].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        int uPixelStride = planes[1].getPixelStride();
        int vPixelStride = planes[2].getPixelStride();
        ByteBuffer yBuffer = planes[0].getBuffer();
        int yBufferSize = yBuffer.capacity() - yBuffer.position();
        //LogUtil.logE("Y ", yRowStride + "   " + yPixelStride + "  " + yBufferSize);
        ByteBuffer uBuffer = planes[1].getBuffer();
        int uBufferSize = uBuffer.capacity() - uBuffer.position();
        //LogUtil.logE("U ", uRowStride + "   " + uPixelStride + "  " + uBufferSize);
        ByteBuffer vBuffer = planes[2].getBuffer();
        int vBufferSize = vBuffer.capacity() - vBuffer.position();
        //LogUtil.logE("V ", vRowStride + "   " + vPixelStride + "  " + vBufferSize);
        if (mCameraNV21Byte == null || mCameraNV21Byte.length < imageWidth * imageHeight * 3 / 2) {
            // allocate NV21 buffer
            mCameraNV21Byte = new byte[imageWidth * imageHeight * 3 / 2];
        }
//        long frameId = atomicLong.getAndIncrement();
        assert (image.getPlanes()[0].getPixelStride() == 1);

        int pos = 0;
        if (yRowStride == imageWidth) { // likely
            yBuffer.get(mCameraNV21Byte, 0, imageWidth * imageHeight);
            pos += imageWidth * imageHeight;
        } else {
            int yBufferPos = 0; // not an actual position
            for (; pos < imageWidth * imageHeight; pos += imageWidth) {
                yBuffer.position(yBufferPos);
                yBuffer.get(mCameraNV21Byte, pos, imageWidth);
                yBufferPos += yRowStride;
            }
        }

//        int inputImageFormat = NV21;
        if (uPixelStride == 1 && vPixelStride == 1) {
            //I420 format, each plane is seperate
            uBuffer.get(mCameraNV21Byte, imageWidth * imageHeight, uBufferSize);
            vBuffer.get(mCameraNV21Byte, imageWidth * imageHeight + imageWidth * imageHeight / 4, vBufferSize);
            //mPixelFormat = 0;
//            inputImageFormat = I420;
//            LogUtil.logI(TAG, "ImageFormat: I420");
        } else if (uPixelStride == 2 && vPixelStride == 2) {
            //NV21 format, UV is packed in one buffer
//            vBuffer.get(mCameraNV21Byte, imageWidth * imageHeight, uBufferSize);
            // uv in one buffer, v buffer is just offset one U pixel
            //mPixelFormat = 1;
            if (uRowStride == imageWidth) {
                vBuffer.get(mCameraNV21Byte, imageWidth * imageHeight, uBufferSize);
            } else {
                pos = 0;
                int vBufferPos = -vRowStride; // not an actual position
                for (int i = 0; i < imageHeight / 2; pos += imageWidth, i++) {
                    vBufferPos += vRowStride;
                    vBuffer.position(vBufferPos);
                    if (i == imageHeight / 2 - 1) {
                        vBuffer.get(mCameraNV21Byte, imageWidth * imageHeight + pos, vBufferSize - vBufferPos);
                    } else {
                        vBuffer.get(mCameraNV21Byte, imageWidth * imageHeight + pos, imageWidth);
                    }
                }
            }
//            inputImageFormat = NV21;
        }
        return mCameraNV21Byte;
    }
    @Override
    public void stopPreview(){
        CameraX.unbind(preview,imageAnalysis);
    }
}
