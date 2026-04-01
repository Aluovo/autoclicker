package com.example.autoclicker1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;

import java.nio.ByteBuffer;

public class ScreenShotHelper {
    public static int resultCode;
    public static Intent resultData;
    public static boolean isReady = false;


    private static MediaProjection mediaProjection;
    private static ImageReader imageReader;

    // ScreenShotHelper.java
    // ScreenShotHelper.java
    // ScreenShotHelper.java
    public static void setUp(Context context, int code, Intent data) {
        android.util.Log.d("TEST", "👉 setUp 被调用了");

        // 获取 Manager
        MediaProjectionManager mpm = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        // ✅ 直接使用传进来的 code 和 data (它们其实就是 resultCode 和 resultData)
        mediaProjection = mpm.getMediaProjection(code, data);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);

        mediaProjection.createVirtualDisplay(
                "screen",
                width,
                height,
                density,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null
        );

        // ✅ 最关键的一步，告诉大家准备好了
        isReady = true;
        android.util.Log.d("TEST", "✅ 初始化完成，isReady=true");
    }
    public static Bitmap getScreenBitmap() {
        if (imageReader == null) return null;

        // 获取最新的一帧
        Image image = imageReader.acquireLatestImage();
        if (image == null) return null;

        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();

        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = planes[0].getPixelStride(); // 像素间距
        int rowStride = planes[0].getRowStride();     // 每行字节数
        int rowPadding = rowStride - pixelStride * width; // 额外填充的字节

        // ✅ 创建一个符合 rowStride 宽度的临时位图
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        image.close(); // 记得关闭 image，否则下次拿不到数据

        // ✅ 裁剪掉右侧多余的 Padding 部分，得到干净的屏幕图片
        return Bitmap.createBitmap(bitmap, 0, 0, width, height);
    }
}