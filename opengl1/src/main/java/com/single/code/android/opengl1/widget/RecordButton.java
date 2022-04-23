package com.single.code.android.opengl1.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatTextView;

/**
 * @author Lance
 * @date 2018/10/8
 */
public class RecordButton extends AppCompatTextView {


    private OnRecordListener mListener;
    private int status = -1;

    public RecordButton(Context context) {
        super(context);
    }

    public RecordButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mListener == null) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(status>0){
                    mListener.onRecordStop();
                    status = 0;
                    setText("开始拍摄");
                }else {
                    mListener.onRecordStart();
                    status = 1;
                    setText("暂停拍摄");
                }
                break;
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//                mListener.onRecordStop();
//                break;
        }
        return true;
    }


    public void setOnRecordListener(OnRecordListener listener) {
        mListener = listener;
    }

    public interface OnRecordListener {
        void onRecordStart();

        void onRecordStop();
    }
}
