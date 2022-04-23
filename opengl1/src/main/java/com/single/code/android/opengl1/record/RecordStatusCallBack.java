package com.single.code.android.opengl1.record;

public interface RecordStatusCallBack {
    void onStart();
    void onComplete(String filePath);
    void onError();
}
