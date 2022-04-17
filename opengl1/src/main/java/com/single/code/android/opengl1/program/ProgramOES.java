package com.single.code.android.opengl1.program;


import android.opengl.GLES20;

/**
 * 创建时间：2022/4/17
 * 创建人：singleCode
 * 功能描述：
 **/
public class ProgramOES extends BaseProgram{
    @Override
    public IDrawable getDrawable() {
        return new DrawableOES();
    }

    @Override
    public void beforeDraw(int texture, float[] mtx) {
        super.beforeDraw(texture, mtx);
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, mtx, 0);
    }
}
