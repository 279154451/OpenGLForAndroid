package com.single.code.android.opengl1.camera;

import android.os.HandlerThread;
import android.util.Size;

import androidx.camera.core.CameraX;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;

import com.single.code.android.opengl1.surface.ISurface;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class CameraXHelper extends BaseCamera{
    private HandlerThread handlerThread;
    private CameraX.LensFacing currentFacing = CameraX.LensFacing.BACK;
    private Preview.OnPreviewOutputUpdateListener listener;
    private Preview preview;
    public CameraXHelper(ISurface surface) {
        super(surface);
    }

    public void startPreview(Preview.OnPreviewOutputUpdateListener listener){
        this.listener = listener;
        handlerThread = new HandlerThread("Analyze-thread");
        handlerThread.start();
        preview = getPreView();
        CameraX.bindToLifecycle(surface.getLifecycleOwner(), preview);
    }
    private Preview getPreView() {
        // 分辨率并不是最终的分辨率，CameraX会自动根据设备的支持情况，结合你的参数，设置一个最为接近的分辨率
        PreviewConfig previewConfig = new PreviewConfig.Builder()
                .setTargetResolution(new Size(640, 480))
                .setLensFacing(currentFacing) //前置或者后置摄像头
                .build();
        Preview preview = new Preview(previewConfig);
        preview.setOnPreviewOutputUpdateListener(listener);
        return preview;
    }
    public void stopPreview(){
        CameraX.unbind(preview);
    }
}
