package com.qi.tai.opengl.base.media.video.provider;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;

import java.io.IOException;

public class AssetsFileProvider extends VideoFileProvider{
    private AssetFileDescriptor fileDescriptor;
    public AssetsFileProvider(Context context, String filePath) {
        super(context, filePath);
        try {
            fileDescriptor = context.getAssets().openFd(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MediaExtractor getMediaExtractor() throws IOException {
        MediaExtractor mMediaExtractor = new MediaExtractor();
        if (fileDescriptor.getDeclaredLength() < 0) {
            mMediaExtractor.setDataSource(fileDescriptor.getFileDescriptor());
        } else {
            mMediaExtractor.setDataSource(fileDescriptor.getFileDescriptor(), fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
        }
        return mMediaExtractor;
    }
}
