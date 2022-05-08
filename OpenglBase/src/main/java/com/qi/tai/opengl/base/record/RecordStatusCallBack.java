package com.qi.tai.opengl.base.record;

public interface RecordStatusCallBack {
    void onStart();
    void onComplete(String filePath);
    void onError();
}
