package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.DisplayActivity.displayManager;
import static com.tpkarras.mirror2rearultra.DisplayActivity.mediaProjectionManager;
import static com.tpkarras.mirror2rearultra.DisplayActivity.resultLauncher;
import static com.tpkarras.mirror2rearultra.QuickTileService.rearDisplayId;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.databinding.ObservableInt;

public class ForegroundService extends Service {

    private static final int ID_SERVICE = 101;
    private ActivityOptions activityOptions;
    public static Context rearDisplay;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static ObservableInt screenRotation = new ObservableInt(0);

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_rear2screenultra)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(ID_SERVICE, notification);
        mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        resultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
        Display[] displays = displayManager.getDisplays();
        rearDisplayId.set(displays[1].getDisplayId());
        rearDisplay = createDisplayContext(displayManager.getDisplay(rearDisplayId.get()));
        activityOptions = activityOptions.makeBasic();
        activityOptions.setLaunchDisplayId(rearDisplayId.get());
        Intent intent = new Intent(rearDisplay, Mirror.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(
                intent,
                activityOptions.toBundle()
        );
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "mirror2rear";
        String channelName = "Mirror2RearUltra Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }
}