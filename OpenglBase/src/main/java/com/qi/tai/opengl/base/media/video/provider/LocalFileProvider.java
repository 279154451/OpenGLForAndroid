package com.qi.tai.opengl.base.media.video.provider;

import android.content.Context;
import android.media.MediaExtractor;

import java.io.IOException;

public class LocalFileProvider extends VideoFileProvider{
    public LocalFileProvider(Context context, String filePath) {
        super(context, filePath);
    }

    @Override
    public MediaExtractor getMediaExtractor() throws IOException {
        MediaExtractor mMediaExtractor = new MediaExtractor();
        mMediaExtractor.setDataSource(filePath);
        return mMediaExtractor;
    }
}
