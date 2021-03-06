package com.tpkarras.mirror2rearultra;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Display;
import android.view.OrientationEventListener;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableInt;

import com.tpkarras.mirror2rearultra.R;

public class ForegroundService extends Service {

    private OrientationEventListener orientationEventListener;
    private static final int ID_SERVICE = 101;

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
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        orientationEventListener = new OrientationEventListener(getApplicationContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                final int rotation = displayManager.getDisplay(displays[0].getDisplayId()).getRotation();
                if(screenRotation.get() != rotation){
                screenRotation.set(rotation);
            }
            }
        };
        orientationEventListener.enable();
    }

    public static ObservableBoolean isScreenPortrait = new ObservableBoolean(true);

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "my_service_channelid";
        String channelName = "My Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }
}