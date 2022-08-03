package com.qi.tai.opengl.base.media.video.provider;

import android.content.Context;
import android.media.MediaExtractor;

import java.io.IOException;

public abstract class VideoFileProvider {
    protected String filePath;

    public VideoFileProvider(Context context,String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void release(){

    }

    public abstract MediaExtractor getMediaExtractor() throws IOException;
}
