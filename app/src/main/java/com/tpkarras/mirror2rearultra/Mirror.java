package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.DisplayActivity.displayManager;
import static com.tpkarras.mirror2rearultra.DisplayActivity.mediaProjection;
import static com.tpkarras.mirror2rearultra.ForegroundService.screenRotation;
import static com.tpkarras.mirror2rearultra.QuickTileService.mirroring;
import static com.tpkarras.mirror2rearultra.QuickTileService.rearDisplayId;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.VirtualDisplay;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Timer;
import java.util.TimerTask;

import dalvik.system.DexClassLoader;

public class Mirror extends Activity {

    private Matrix matrix;
    private TextureView textureView;
    private VirtualDisplay virtualDisplay;
    private long timeout;
    private Timer t;

    public void subscreenDisplayTrigger(boolean trigger) {
            try {
                SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
                Sensor screenDown = sensorManager.getDefaultSensor(33171037);
                SensorEventListener sensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        subscreenDisplayTrigger(true);
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }
                };
                DexClassLoader dexClassLoader = new DexClassLoader("/system/app/MiSubScreenUi/MiSubScreenUi.apk", getCodeCacheDir().getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
                Class<Object> subscreenManager = (Class<Object>) dexClassLoader.loadClass("com.xiaomi.misubscreenui.manager.DeviceManager");
                Constructor[] subscreenct = subscreenManager.getDeclaredConstructors();
                Object subscreenInstance = subscreenct[0].newInstance(getApplicationContext());
                if (trigger == true) {
                    subscreenManager.getMethod("wakeupScreen").invoke(subscreenInstance);
                    sensorManager.registerListener(sensorEventListener, screenDown, 0);
                } else {
                    sensorManager.unregisterListener(sensorEventListener);
                    subscreenManager.getMethod("letSubScreenOff").invoke(subscreenInstance);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    protected void onCreate(Bundle savedInstanceState) {
        try {
            timeout = Settings.System.getLong(getContentResolver(), "subscreen_display_time");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        Timer t = new Timer();
        Window window = getWindow();
        Matrix matrix = new Matrix();
        if(screenRotation.get() == 0 || screenRotation.get() == 2) {
            virtualDisplay = mediaProjection.createVirtualDisplay("Mirror",
                    126, 294, 290,
                    displayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR | displayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    null, null, null);
        } else if(screenRotation.get() == 3 || screenRotation.get() == 1) {
            virtualDisplay = mediaProjection.createVirtualDisplay("Mirror",
                    294, 126, 290,
                    displayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR | displayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    null, null, null);
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        window.setContentView(R.layout.mirror_surface);
        window.setAttributes(layoutParams);
        textureView = findViewById(R.id.mirror);
        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                Surface surface = new Surface(surfaceTexture);
                virtualDisplay.setSurface(surface);
                if (screenRotation.get() == 0) {
                    matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                } else if (screenRotation.get() == 3) {
                    matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                    matrix.postTranslate(-167, 0);
                } else if (screenRotation.get() == 1) {
                    matrix.setRotate(-90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                } else if (screenRotation.get() == 2) {
                    matrix.setRotate(-180, textureView.getWidth() / 2, textureView.getHeight() / 2);
                    matrix.postTranslate(-167, 0);
                }
                textureView.setTransform(matrix);
                screenRotation.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable observable, int i) {
                        if (screenRotation.get() == 0) {
                            virtualDisplay.resize(126, 294, 290);
                            matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        } else if (screenRotation.get() == 3) {
                            virtualDisplay.resize(294, 126, 290);
                            matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            matrix.postTranslate(-167, 0);
                        } else if (screenRotation.get() == 1) {
                            virtualDisplay.resize(294, 126, 290);
                            matrix.setRotate(-90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        } else if (screenRotation.get() == 2) {
                            virtualDisplay.resize(126, 294, 290);
                            matrix.setRotate(-180, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            matrix.postTranslate(-167, 0);
                        }
                        textureView.setTransform(matrix);
                        subscreenDisplayTrigger(true);
                    }
                });
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        };
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    StringBuilder string = new StringBuilder("input -d ");
                    string.append(rearDisplayId.get());
                    string.append(" tap 0 0");
                    Runtime.getRuntime().exec(string.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, timeout / 3);
        mirroring.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (mirroring.get() == 0) {
                    subscreenDisplayTrigger(false);
                    finish();
                }
            }
        });
    }

    public void onPause() {
        super.onPause();

    }

    public void onResume() {
        super.onResume();
        subscreenDisplayTrigger(true);
    }
}
