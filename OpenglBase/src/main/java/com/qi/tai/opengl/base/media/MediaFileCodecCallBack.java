package com.qi.tai.opengl.base.media;


public interface MediaFileCodecCallBack {

    void onFrame(FileRenderOutputData outputData);

    void onStop(boolean isBackground,boolean isRelease);
    void onRelease(boolean isBackground, IMediaFileCodec mediaFileCodec);
    void onStart(String filePath,boolean restart);
}
