package com.single.code.android.opengl;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.single.code.android.opengl1.Opengl1Activity;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.security.Permission;
import java.security.Permissions;

public class MainActivity extends AppCompatActivity {
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        findViewById(R.id.btn_opengl1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(Opengl1Activity.class);
            }
        });
        requestPermission();
    }

    private void requestPermission(){
        RxPermissions rxPermissions = new RxPermissions(this);
        rxPermissions.request( Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    Log.i("cxw","申请结果:"+granted);
                });
    }

    private void startActivity(Class<?> clazz){
        Intent intent = new Intent(context, clazz);
        startActivity(intent);
    }
}