package com.qi.tai.opengl.base.media.video.provider;

import android.content.Context;
import android.media.MediaExtractor;
import android.net.Uri;

import java.io.IOException;

public class RawFileProvider extends VideoFileProvider{
    private Context context;
    public RawFileProvider(Context context, String filePath) {
        super(context, filePath);
        this.context = context.getApplicationContext();
    }

    @Override
    public MediaExtractor getMediaExtractor() throws IOException {
        MediaExtractor mMediaExtractor = new MediaExtractor();
        final Uri uri = Uri.parse(filePath);
        mMediaExtractor.setDataSource(context, uri, null);
        return mMediaExtractor;
    }
}
