package com.qi.tai.opengl.base.media;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.Matrix;

import java.nio.ByteBuffer;

public class FileRenderOutputData {
    private int width;
    private int height;
    private int textId;
    private SurfaceTexture surfaceTexture;
    private ByteBuffer outputBuffer;
    private boolean isBackground = false;
    private Bitmap bitmap;
    private boolean isPlaying = false;
    private byte[] frameBytes;
    private int rotation;
    private String filePath;
    public FileRenderOutputData(int width, int height, int textId,boolean isBackground,int rotation,SurfaceTexture surfaceTexture) {
        this.width = width;
        this.height = height;
        this.textId = textId;
        this.surfaceTexture = surfaceTexture;
        this.isBackground = isBackground;
        this.rotation = rotation;
    }

    public int getRotation() {
        return rotation;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public FileRenderOutputData(Bitmap bitmap, boolean isBackground) {
        this.bitmap = bitmap;
        this.isBackground = isBackground;
    }
    public FileRenderOutputData(int width,int height,byte[] buffer){
        this.width = width;
        this.height = height;
        this.frameBytes = buffer;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBackground(boolean background) {
        isBackground = background;
    }

    public boolean isBackground() {
        return isBackground;
    }

    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }

    public void setFrameBytes(byte[] frameBytes) {
        this.frameBytes = frameBytes;
    }

    public byte[] getFrameBytes() {
        return frameBytes;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getTextId() {
        return textId;
    }

    public void setOutputBuffer(ByteBuffer outputBuffer) {
        this.outputBuffer = outputBuffer;
    }

    public ByteBuffer getOutputBuffer() {
        return outputBuffer;
    }

    public void setTextId(int textId) {
        this.textId = textId;
    }
    public float[] updateTexImage() {
        float[] texMvp = new float[16];
        Matrix.setIdentityM(texMvp, 0);
        if (surfaceTexture != null) {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(texMvp);
        }
        return texMvp;
    }



    @Override
    public String toString() {
        return "FileRenderOutputData{" +
                "width=" + width +
                ", height=" + height +
                ", textId=" + textId +
                ", surfaceTexture=" + surfaceTexture +
                ", outputBuffer=" + outputBuffer +
                ", isBackground=" + isBackground +
                ", filePath="+filePath+
                '}';
    }
}
