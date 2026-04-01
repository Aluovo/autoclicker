package com.example.autoclicker1;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
public class MainActivity extends AppCompatActivity {

    private MediaProjectionManager mpm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // 请求录屏权限
        startActivityForResult(mpm.createScreenCaptureIntent(), 1);
    }

    // MainActivity.java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            android.util.Log.d("TEST", "👉 拿到录屏授权了，开始存入全局变量");

            // ✅ 核心 1：直接存入静态变量
            ScreenShotHelper.resultCode = resultCode;
            ScreenShotHelper.resultData = data;

            // ✅ 核心 2：启动 Service 时，什么数据都不带
            Intent intent = new Intent(this, FloatService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }
    }
}