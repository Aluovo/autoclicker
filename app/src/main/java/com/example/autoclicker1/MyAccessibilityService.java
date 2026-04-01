package com.example.autoclicker1;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;

public class MyAccessibilityService extends AccessibilityService {

    public static MyAccessibilityService instance;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    // ✅ 全新的智能动作执行器（兼容点击与滑动，带防屏蔽按压时长）
    public void performAction(int startX, int startY, int endX, int endY) {
        Path path = new Path();
        path.moveTo(startX, startY);

        // 🌟🌟🌟 核心修改点 🌟🌟🌟
        // 将普通点击的按压时长从 100 毫秒直接提升到了 300 毫秒！//又改成150ms了
        // 这是一个完美的“真实人类按压”时长，Unity 引擎绝对无法免疫。
        int duration = 150;

        // 如果起点和终点距离超过 20 个像素，说明你在滑动
        if (Math.abs(startX - endX) > 20 || Math.abs(startY - endY) > 20) {
            path.lineTo(endX, endY); // 划一条线到终点
            // 滑动动作需要更长的时间来完成，否则会变成瞬移，游戏识别不出
            duration = 600;
        }

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, duration));

        dispatchGesture(builder.build(), null, null);
    }

    // 保留老的 click 方法，防止其他地方报错，直接桥接到 performAction
    public void click(int x, int y) {
        performAction(x, y, x, y);
    }
}