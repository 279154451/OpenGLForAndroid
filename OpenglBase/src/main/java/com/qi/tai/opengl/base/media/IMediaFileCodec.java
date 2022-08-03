package com.qi.tai.opengl.base.media;

public interface IMediaFileCodec {
    void setCodecCallBack(MediaFileCodecCallBack codecCallBack);
    void init();
    void release();
    boolean isRelease();
    void start();
}
