package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.DisplayActivity.displayManager;
import static com.tpkarras.mirror2rearultra.DisplayActivity.mediaProjection;
import static com.tpkarras.mirror2rearultra.ForegroundService.screenRotation;
import static com.tpkarras.mirror2rearultra.QuickTileService.mirroring;
import static com.tpkarras.mirror2rearultra.QuickTileService.rearDisplayId;

import android.content.ContentProvider;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import android.os.Bundle;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import dalvik.system.DexClassLoader;

public class Mirror extends Activity {

    private Matrix matrix;
    private TextureView textureView;
    private VirtualDisplay virtualDisplay;
    private Window window;

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
        super.onCreate(savedInstanceState);
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
        window = this.getWindow();
        //WindowManager.LayoutParams layoutParams = window.getAttributes();
        //layoutParams.buttonBrightness = 1f;
        //window.setAttributes(layoutParams);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.setContentView(R.layout.mirror_surface);
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
    }

    public void onPause() {
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        subscreenDisplayTrigger(true);
    }
}
