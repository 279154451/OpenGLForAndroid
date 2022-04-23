package com.single.code.android.opengl1;

import android.os.Bundle;
import android.os.Environment;
import android.widget.RadioGroup;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.single.code.android.opengl1.record.RecordManager;
import com.single.code.android.opengl1.widget.RecordButton;

import java.io.File;

/**
 * 创建时间：2022/4/16
 * 创建人：singleCode
 * 功能描述：
 **/
public class Opengl1Activity extends AppCompatActivity implements RecordButton.OnRecordListener, RadioGroup.OnCheckedChangeListener {
    @Override
    protected void onCreate(@Nullable  Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opengl1);
        RecordButton btn_record = findViewById(R.id.btn_record);
        btn_record.setOnRecordListener(this);

        //速度
        RadioGroup rgSpeed = findViewById(R.id.rg_speed);
        rgSpeed.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (checkedId == R.id.btn_extra_slow) {
            RecordManager.getManager().setSpeed(RecordManager.Speed.MODE_EXTRA_SLOW);
        } else if (checkedId == R.id.btn_slow) {
            RecordManager.getManager().setSpeed(RecordManager.Speed.MODE_SLOW);
        } else if (checkedId == R.id.btn_normal) {
            RecordManager.getManager().setSpeed(RecordManager.Speed.MODE_NORMAL);
        } else if (checkedId == R.id.btn_fast) {
            RecordManager.getManager().setSpeed(RecordManager.Speed.MODE_FAST);
        } else if (checkedId == R.id.btn_extra_fast) {
            RecordManager.getManager().setSpeed(RecordManager.Speed.MODE_EXTRA_FAST);
        }
    }

    @Override
    public void onRecordStart() {
        String filePath = ContextCompat.getExternalFilesDirs(this, Environment.DIRECTORY_DCIM)[0].getAbsolutePath()+ File.separator+"a.mp4";
        RecordManager.getManager().startRecord(filePath);
    }

    @Override
    public void onRecordStop() {
        RecordManager.getManager().stopRecord();
    }
}
