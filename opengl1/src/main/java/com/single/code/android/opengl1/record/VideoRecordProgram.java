package com.single.code.android.opengl1.record;

import com.single.code.android.opengl1.program.BaseProgram;
import com.single.code.android.opengl1.program.DrawableScreen2D;
import com.single.code.android.opengl1.program.IDrawable;

public class VideoRecordProgram extends BaseProgram {
    @Override
    public IDrawable getDrawable() {
        return new DrawableScreen2D();
    }
    @Override
    public void beforeDraw(int texture, float[] mtx) {
        //这里不用进行矩阵变换，因为在之前已经变换过了

    }
}
