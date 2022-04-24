package com.single.code.android.opengl1.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;


import com.single.code.android.opengl1.surface.GLView;
import com.single.code.android.opengl1.utils.Constants;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class Camera2Helper extends BaseCamera implements ImageReader.OnImageAvailableListener{
    /**
     * 输入的图片类型
     */
    public static final int I420 = ImageFormat.YUV_420_888;
    public static final int NV21 = ImageFormat.NV21;

    private static final String TAG = Camera2Helper.class.toString();
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);//许可证管理器
    private CameraCaptureSession mCaptureSession;//这个对象控制摄像头的预览或者拍照
    private CameraDevice mCameraDevice;//摄像头
    /**
     * LENS_FACING_BACK： 前置摄像头
     * LENS_FACING_FRONT：后置摄像头
     */
    private int mCameraId = CameraCharacteristics.LENS_FACING_BACK;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    private ImageReader mImageReader;

    private WeakReference<Context> activityWeakReference;
    private int mCameraOrientation;//图片旋转的角度
    private final Object mCameraLock = new Object();
    /**
     * 相机输出的画面载体
     */
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private int mCameraTextureId;//相机纹理
    /**
     * 相机回调
     */
    private volatile byte[] mCameraNV21Byte;
    private CameraListener cameraListener;
    private int inputImageFormat = NV21;
    private AtomicLong atomicLong = new AtomicLong();

    public Camera2Helper(GLView glView,IPreviewOutputUpdateListener listener) {
        super(glView,listener);
        activityWeakReference = new WeakReference<>(glView.getContext());
    }

    public void setCameraListener(CameraListener cameraListener) {
        this.cameraListener = cameraListener;
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    /**
     * gl环境创建完成之后设置
     * gl环境销毁之后，设置为0
     *
     * @param mCameraTextureId
     */
    public void setCameraTextureId(int mCameraTextureId) {
        this.mCameraTextureId = mCameraTextureId;
    }

    /**
     * 打开前置摄像机
     */
    public void openCameraFront() {
        mCameraId = CameraCharacteristics.LENS_FACING_BACK;
        openCamera();
    }

    /**
     * 开启相机
     */
    public void openCamera() {
        openCamera(Constants.mCameraWidth, Constants.mCameraHeight);
    }

    /**
     * 切换摄像头
     */
    public void changeCamera() {
        releaseCamera();
        mCameraId = mCameraId == CameraCharacteristics.LENS_FACING_BACK ?
                CameraCharacteristics.LENS_FACING_FRONT :
                CameraCharacteristics.LENS_FACING_BACK;
        openCamera(Constants.mCameraWidth, Constants.mCameraHeight);
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    public void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    public void stopBackgroundThread() {
        if (mBackgroundThread == null)
            return;
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 打开相机
     *
     * @param mCameraWidth  相机预览的宽
     * @param mCameraHeight 相机预览的高
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void openCamera(int mCameraWidth, int mCameraHeight) {

//        if (activityWeakReference.get() == null || mCameraTextureId == 0 || ContextCompat.checkSelfPermission(activityWeakReference.get(), Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        if (activityWeakReference.get() == null || ContextCompat.checkSelfPermission(activityWeakReference.get(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
//        CameraDataManager.getInstance().init();
        startBackgroundThread();
        Log.d(TAG, "openCamera: mCameraWidth=" + mCameraWidth
                + " mCameraHeight=" + mCameraHeight);
        if (null == mImageReader) {
            // maxImages参数的值需要根据调用的是ImageReader的acquireLatestImage方法还是acquireNextImage方法来决定，
            // 调用acquireLatestImage方法的时候 ，maxImages的值需要大于1
            mImageReader = ImageReader.newInstance(mCameraWidth, mCameraHeight, ImageFormat.YUV_420_888, 2);
            mImageReader.setOnImageAvailableListener(this, mBackgroundHandler);
        }
        //摄像头管理器
        CameraManager manager = (CameraManager) activityWeakReference.get().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId + "");
            /**
             * 手机支持的图片格式为：
             * JPEG(256),YUV_420_888(35),PRIVATE(34),DEPTH16(1144402265);
             */
//            StreamConfigurationMap streamConfigurationMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//            int[] outputFormats = streamConfigurationMap.getOutputFormats();
//            for (int j = 0; j < outputFormats.length; j++) {
//                LogUtil.logI(TAG, "outputFormat[" + j + "]=" + outputFormats[j]);
//            }
            mCameraOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (cameraListener != null) {
                cameraListener.onCameraChange(isCameraFront(), mCameraOrientation);
            }
            Log.i(TAG, "mCameraOrientation:" + mCameraOrientation);
            manager.openCamera(mCameraId + "", mStateCallback, mBackgroundHandler);//打开相机
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot access the camera." + e.toString());
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * 摄像头创建监听
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            //创建摄像头的预览会话
            createCaptureSession();
            if(cameraListener!= null){
                cameraListener.onCameraOpen(isCameraFront());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
    };


    /**
     * 创建摄像头的预览会话
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createCaptureSession() {
        try {
            if (null == mCameraDevice || null == mImageReader) return;
            ArrayList<Surface> views = new ArrayList<>();
            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
                mSurface.release();
            }
//            mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
            mSurfaceTexture = new SurfaceTexture(false);
            mSurfaceTexture.setDefaultBufferSize(Constants.mCameraWidth, Constants.mCameraHeight);
            mSurface = new Surface(mSurfaceTexture);
            views.add(mSurface);
            views.add(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(views,
                    sessionStateCallback, mBackgroundHandler);
            if(listener!= null){
                listener.onUpdate(mSurfaceTexture);
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "createCaptureSession " + e.toString());
        }
    }

    /**
     * 创建摄像头的预览会话监听
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            mCaptureSession = session;
            try {
                CaptureRequest captureRequest = createCaptureRequest();
                if (captureRequest != null) {
                    //不断发送请求
                    session.setRepeatingRequest(captureRequest, null, mBackgroundHandler);
                } else {
                    Log.e(TAG, "captureRequest is null");
                }
            } catch (CameraAccessException e) {
                Log.e(TAG, "onConfigured " + e.toString());
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.e(TAG, "onConfigureFailed");
        }

        @Override
        public void onClosed(@NonNull CameraCaptureSession session) {
            super.onClosed(session);
            stopBackgroundThread();
        }
    };

    /**
     * 一次捕获请求
     *
     * @return
     */
    private CaptureRequest createCaptureRequest() {
        if (null == mCameraDevice) return null;
        try {
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(mSurface);
            builder.addTarget(mImageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException:" + e.getMessage());
            return null;
        }
    }

    /**
     * 关闭相机
     */
    public void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if (mSurface != null) {
                mSurfaceTexture.release();
                mSurface.release();
                mSurfaceTexture = null;
                mSurface = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
        Log.i(TAG, "closeCamera");
    }

    public byte[] getCameraNV21Byte() {
//        return CameraDataManager.getInstance().getCameraNV21Byte();
        return mCameraNV21Byte;
    }

    public boolean isOpen() {
        return mBackgroundThread != null;
    }

    /**
     * 相机预览的宽
     *
     * @return
     */
    public int getCameraWidth() {
        return Constants.mCameraWidth;
    }

    /**
     * 相机预览的高
     *
     * @return
     */
    public int getCameraHeight() {
        return Constants.mCameraHeight;
    }

    /**
     * 是否是前置摄像头
     *
     * @return
     */
    public boolean isCameraFront() {
        return mCameraId == CameraCharacteristics.LENS_FACING_BACK;
    }

    /**
     * 获取相机角度
     *
     * @return
     */
    public int getCameraOrientation() {
        return mCameraOrientation;
    }

    public void updateTexImage(float[] texMvp) {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
            mSurfaceTexture.getTransformMatrix(texMvp);
        }
    }

    public Surface getSurface() {
        return mSurface;
    }

    public int getInputImageFormat() {
        return inputImageFormat;
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();
        if (image == null) {
            return;
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
        long frameId = atomicLong.getAndIncrement();
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

        int inputImageFormat = NV21;
        if (uPixelStride == 1 && vPixelStride == 1) {
            //I420 format, each plane is seperate
            uBuffer.get(mCameraNV21Byte, imageWidth * imageHeight, uBufferSize);
            vBuffer.get(mCameraNV21Byte, imageWidth * imageHeight + imageWidth * imageHeight / 4, vBufferSize);
            //mPixelFormat = 0;
            inputImageFormat = I420;
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


            inputImageFormat = NV21;
//            LogUtil.logI(TAG, "ImageFormat: NV21");
        }
//        CameraDataManager.getInstance().addDataToQueue(mCameraNV21Byte, frameId);
        image.close();
        this.inputImageFormat = inputImageFormat;
        if(inputImageFormat == NV21){
//            Log.d(TAG, "onImageAvailable: nv21");
        }else {
//            Log.d(TAG, "onImageAvailable: I420");
        }
        if (cameraListener != null) {
            cameraListener.onPreviewFrame();
        }
    }

    @Override
    public void startPreview() {
        openCamera();
    }

    @Override
    public void stopPreview() {
        releaseCamera();
    }

    /**
     * 相机回调
     * <p>
     * onCameraChange相机方向和角度改变回调
     * onPreviewFrame预览buffer数据回调
     */
    public interface CameraListener {
        void onCameraOpen(boolean isFront);

        void onCameraChange(boolean isFront, int cameraOrientation);

        void onPreviewFrame();
    }

    /**
     * 释放相机
     */
    public void releaseCamera() {
        try {
            synchronized (mCameraLock) {
//                CameraDataManager.getInstance().release();
                closeCamera();
                stopBackgroundThread();
                mCameraNV21Byte = null;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
