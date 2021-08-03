package com.crazyxacker.libs.infowidgets;

import android.content.Context;
import android.net.TrafficStats;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import com.crazyxacker.libs.interfaces.INetSpeedCallback;

import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetSpeedMeter {
    private final Context mContext;
    private final TextView mSpeedView;
    private final Handler mMainHandler;
    private ScheduledExecutorService mSpeedTimer;
    private Long lastTotalRxBytes = 0L, lastTimeStamp = 0L;

    private final String kbLocalized;
    private final String mbLocalized;

    public NetSpeedMeter(Context context, TextView speedView, INetSpeedCallback callback) {
        mContext = context;
        mSpeedView = speedView;
        mMainHandler = new Handler(Looper.getMainLooper());

        Runnable netSpeedSetTask = () -> mMainHandler.post(() -> mSpeedView.post(() -> {
            if (callback.isShowNetSpeed()) {
                mSpeedView.setVisibility(View.VISIBLE);
                mSpeedView.setText(getNetSpeed());
            } else {
                mSpeedView.setVisibility(View.GONE);
            }
        }));
        mSpeedTimer = Executors.newScheduledThreadPool(2);
        mSpeedTimer.scheduleWithFixedDelay(netSpeedSetTask, 400, 300, TimeUnit.MILLISECONDS);

        kbLocalized = context.getString(R.string.net_speed_kb_s);
        mbLocalized = context.getString(R.string.net_speed_mb_s);
    }

    public String getNetSpeed() {
        String netSpeed;
        long nowTotalRxBytes = getTotalRxBytes(mContext);
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = (nowTimeStamp - lastTimeStamp);
        if (calculationTime == 0) {
            netSpeed = String.format("%s %s", 1, kbLocalized);
            return netSpeed;
        }
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime);
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        if (speed > 1024) {
            DecimalFormat df = new DecimalFormat("######0.0");
            netSpeed = String.format("%s %s", df.format(getM(speed)), mbLocalized);
        } else {
            netSpeed = String.format("%s %s", speed, kbLocalized);
        }
        return netSpeed;
    }

    public static long getTotalRxBytes(Context context) {
        return TrafficStats.getUidRxBytes(context.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED
                ? 0
                : (TrafficStats.getTotalRxBytes() / 1024);
    }

    public static double getM(long k) {
        return k / 1024.0;
    }

    public void onDestroy() {
        if (mSpeedTimer != null && !mSpeedTimer.isShutdown()) {
            mSpeedTimer.shutdownNow();
        }
        mSpeedTimer = null;
    }
}
