package com.example.autoclicker1;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FloatService extends Service {

    private WindowManager windowManager;
    private View floatView;
    private Button mainBtn;
    private View overlayView = null;

    private final List<int[]> pointList = new ArrayList<>();
    private SharedPreferences prefs;

    private boolean isFinding = false;

    @Override
    public void onCreate() {
        super.onCreate();

        String channelId = "screen_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "全能连点器", android.app.NotificationManager.IMPORTANCE_LOW);
            getSystemService(android.app.NotificationManager.class).createNotificationChannel(channel);
        }
        android.app.Notification notification = new android.app.Notification.Builder(this, channelId)
                .setContentTitle("全能外挂中枢运行中")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .build();
        startForeground(1, notification);

        prefs = getSharedPreferences("MacroPrefs", MODE_PRIVATE);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatView = LayoutInflater.from(this).inflate(R.layout.float_layout, null);

        WindowManager.LayoutParams floatParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        floatParams.x = 100;
        floatParams.y = 300;
        windowManager.addView(floatView, floatParams);

        mainBtn = floatView.findViewById(R.id.btn);
        mainBtn.setText("⚙️ 主菜单");

        mainBtn.setOnClickListener(v -> {
            String text = mainBtn.getText().toString();
            if (text.equals("⚙️ 主菜单")) {
                showMenuDialog();
            } else if (text.equals("⏹️ 结束录制")) {
                endRecordingAndSave();
            } else if (text.equals("⏹️ 停止挂机")) {
                isFinding = false;
                resetToMenu();
            } else if (text.startsWith("▶️")) {
                executeScript();
            }
        });

        mainBtn.setOnLongClickListener(v -> {
            isFinding = false;
            resetToMenu();
            Toast.makeText(this, "已强行停止", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private void showMenuDialog() {
        Set<String> scriptNames = prefs.getStringSet("script_names", new HashSet<>());

        List<String> displayList = new ArrayList<>();
        displayList.add("➕ 新建动作录制 (点击/滑动)");
        displayList.add("👁️ 134步序列找图 (带自适应坐标映射)");
        displayList.add("🎮 智能收菜模式 (优先点 SSR > SR)");
        displayList.addAll(scriptNames);

        String[] items = displayList.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        builder.setTitle("选择挂机功能");

        builder.setItems(items, (dialog, which) -> {
            if (which == 0) startRecording();
            else if (which == 1) startImageSequenceLoop();
            else if (which == 2) startSmartFarmLoop();
            else loadScript(items[which]);
        });

        if (!scriptNames.isEmpty()) {
            builder.setNeutralButton("🗑️ 清空存档", (dialog, which) -> {
                prefs.edit().clear().apply();
                Toast.makeText(this, "存档已清空", Toast.LENGTH_SHORT).show();
            });
        }
        builder.setNegativeButton("取消", null);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }

    // ==========================================
    // 模式一：固定坐标录制 (省略无修改部分...)
    // ==========================================
    private void startRecording() { /* 保持原样，字数限制略 */ }
    private void endRecordingAndSave() { /* 保持原样，字数限制略 */ }
    private void loadScript(String name) { /* 保持原样，字数限制略 */ }
    private void executeScript() { /* 保持原样，字数限制略 */ }

    // ==========================================
    // 模式二：序列找图挂机 (🌟 加入坐标等比映射缩放)
    // ==========================================
    private void startImageSequenceLoop() {
        if (!ScreenShotHelper.isReady) {
            Toast.makeText(this, "录屏未就绪，请重启App授予权限", Toast.LENGTH_SHORT).show();
            return;
        }

        // 👇👇👇 状态栏偏移补偿 👇👇👇
        // 如果开启“指针位置”后，发现点击位置始终比你的目标偏上了一截（通常是因为刘海屏/状态栏），请把 0 改成 80 或 100
        final int STATUS_BAR_OFFSET_Y = 0;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        final List<Bitmap> sequenceImages = new ArrayList<>();
        for (int i = 1; i <= 300; i++) {
            int resId = getResources().getIdentifier("image" + i, "drawable", getPackageName());
            if (resId == 0) resId = getResources().getIdentifier("step" + i, "drawable", getPackageName());
            if (resId != 0) {
                Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId, options);
                if (bmp != null) sequenceImages.add(bmp);
            }
        }

        if (sequenceImages.isEmpty()) return;

        isFinding = true;
        mainBtn.setText("⏹️ 停止挂机");
        Toast.makeText(this, "序列启动！有效动作: " + sequenceImages.size(), Toast.LENGTH_SHORT).show();

        // 获取手机真实的物理分辨率
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        final int realScreenWidth = metrics.widthPixels;
        final int realScreenHeight = metrics.heightPixels;

        new Thread(() -> {
            int currentStepIndex = 0;
            int notFoundCount = 0;

            while (isFinding) {
                Bitmap screenBmp = ScreenShotHelper.getScreenBitmap();
                if (screenBmp != null) {
                    Bitmap currentTarget = sequenceImages.get(currentStepIndex);
                    int[] center = findImageCenter(screenBmp, currentTarget);

                    if (center != null) {
                        android.util.Log.d("TEST", "🎯 找到目标 " + (currentStepIndex + 1));
                        notFoundCount = 0;

                        if (MyAccessibilityService.instance != null) {
                            // 🌟 核心修复：计算截图与真实屏幕的缩放比例！
                            float scaleX = (float) realScreenWidth / screenBmp.getWidth();
                            float scaleY = (float) realScreenHeight / screenBmp.getHeight();

                            // 将图片坐标映射到真实屏幕坐标，并加上状态栏补偿
                            int finalClickX = (int) (center[0] * scaleX);
                            int finalClickY = (int) (center[1] * scaleY) + STATUS_BAR_OFFSET_Y;

                            android.util.Log.d("TEST", "👆 映射后最终点击坐标: " + finalClickX + ", " + finalClickY);
                            MyAccessibilityService.instance.click(finalClickX, finalClickY);
                        }

                        currentStepIndex++;
                        if (currentStepIndex >= sequenceImages.size()) currentStepIndex = 0;
                        try { Thread.sleep(2500); } catch (Exception ignored) {}
                    } else {
                        notFoundCount++;
                        if (notFoundCount == 4) {
                            if (MyAccessibilityService.instance != null) {
                                MyAccessibilityService.instance.performAction(realScreenWidth / 2, (int)(realScreenHeight * 0.7), realScreenWidth / 2, (int)(realScreenHeight * 0.3));
                            }
                            try { Thread.sleep(1500); } catch (Exception ignored) {}
                        }
                        if (notFoundCount >= 8) {
                            currentStepIndex++;
                            notFoundCount = 0;
                            if (currentStepIndex >= sequenceImages.size()) currentStepIndex = 0;
                        }
                    }
                }
                try { Thread.sleep(1000); } catch (Exception ignored) {}
            }
        }).start();
    }

    // ==========================================
    // 模式三：智能收菜 (同样加入缩放映射)
    // ==========================================
    private void startSmartFarmLoop() {
        if (!ScreenShotHelper.isReady) return;

        final int FARM_OFFSET_X = 300;
        final int FARM_OFFSET_Y = 250;
        final int STATUS_BAR_OFFSET_Y = 0;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        final Bitmap bmpSSR = BitmapFactory.decodeResource(getResources(), R.drawable.anchor_ssr, options);
        final Bitmap bmpSR = BitmapFactory.decodeResource(getResources(), R.drawable.anchor_sr, options);
        final Bitmap bmpR = BitmapFactory.decodeResource(getResources(), R.drawable.anchor_r, options);
        final Bitmap bmpRefresh = BitmapFactory.decodeResource(getResources(), R.drawable.btn_refresh, options);

        isFinding = true;
        mainBtn.setText("⏹️ 停止挂机");

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        final int realScreenWidth = metrics.widthPixels;
        final int realScreenHeight = metrics.heightPixels;

        new Thread(() -> {
            while (isFinding) {
                Bitmap screenBmp = ScreenShotHelper.getScreenBitmap();
                if (screenBmp != null) {
                    float scaleX = (float) realScreenWidth / screenBmp.getWidth();
                    float scaleY = (float) realScreenHeight / screenBmp.getHeight();

                    int[] ssrCenter = findImageCenter(screenBmp, bmpSSR);
                    if (ssrCenter != null) {
                        int finalX = (int) ((ssrCenter[0] + FARM_OFFSET_X) * scaleX);
                        int finalY = (int) ((ssrCenter[1] + FARM_OFFSET_Y) * scaleY) + STATUS_BAR_OFFSET_Y;
                        if (MyAccessibilityService.instance != null) MyAccessibilityService.instance.click(finalX, finalY);
                        try { Thread.sleep(3000); } catch (Exception ignored) {} continue;
                    }

                    int[] srCenter = findImageCenter(screenBmp, bmpSR);
                    if (srCenter != null) {
                        int finalX = (int) ((srCenter[0] + FARM_OFFSET_X) * scaleX);
                        int finalY = (int) ((srCenter[1] + FARM_OFFSET_Y) * scaleY) + STATUS_BAR_OFFSET_Y;
                        if (MyAccessibilityService.instance != null) MyAccessibilityService.instance.click(finalX, finalY);
                        try { Thread.sleep(3000); } catch (Exception ignored) {} continue;
                    }

                    // R和刷新的逻辑同理... (保持原有逻辑)
                }
                try { Thread.sleep(1000); } catch (Exception ignored) {}
            }
        }).start();
    }

    // ==========================================
    // 核心视觉识别算法 (终极内缩 5 点防抖引擎)
    // ==========================================
    private int[] findImageCenter(Bitmap screen, Bitmap template) {
        if (screen == null || template == null) return null;

        int sw = screen.getWidth(), sh = screen.getHeight();
        int tw = template.getWidth(), th = template.getHeight();
        if (tw > sw || th > sh) return null;

        int cx = tw / 2, cy = th / 2;
        int qx1 = tw / 4, qy1 = th / 4;
        int qx3 = tw * 3 / 4, qy3 = th * 3 / 4;

        int pCenter = template.getPixel(cx, cy);
        int pTL = template.getPixel(qx1, qy1);
        int pTR = template.getPixel(qx3, qy1);
        int pBL = template.getPixel(qx1, qy3);
        int pBR = template.getPixel(qx3, qy3);

        int tolerance = 25;

        for (int y = 0; y <= sh - th; y += 3) {
            for (int x = 0; x <= sw - tw; x += 3) {
                if (!isColorMatch(screen.getPixel(x + cx, y + cy), pCenter, tolerance)) continue;
                if (!isColorMatch(screen.getPixel(x + qx1, y + qy1), pTL, tolerance)) continue;
                if (!isColorMatch(screen.getPixel(x + qx3, y + qy1), pTR, tolerance)) continue;
                if (!isColorMatch(screen.getPixel(x + qx1, y + qy3), pBL, tolerance)) continue;
                if (!isColorMatch(screen.getPixel(x + qx3, y + qy3), pBR, tolerance)) continue;

                return new int[]{x + cx, y + cy};
            }
        }
        return null;
    }

    private boolean isColorMatch(int c1, int c2, int tol) {
        return Math.abs(android.graphics.Color.red(c1) - android.graphics.Color.red(c2)) <= tol &&
                Math.abs(android.graphics.Color.green(c1) - android.graphics.Color.green(c2)) <= tol &&
                Math.abs(android.graphics.Color.blue(c1) - android.graphics.Color.blue(c2)) <= tol;
    }

    // 状态重置与生命周期
    private void resetToMenu() {
        if (overlayView != null) {
            try { windowManager.removeView(overlayView); } catch (Exception ignored) {}
            overlayView = null;
        }
        pointList.clear();
        mainBtn.setText("⚙️ 主菜单");
        mainBtn.setEnabled(true);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ScreenShotHelper.resultData != null) ScreenShotHelper.setUp(this, ScreenShotHelper.resultCode, ScreenShotHelper.resultData);
        return START_NOT_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) { return null; }
}