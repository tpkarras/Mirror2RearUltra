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
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dalvik.system.DexClassLoader;

public class Mirror extends Activity {
    public Class<Object> subscreenManager;
    public Constructor[] subscreenct;
    public Object subscreenInstance;
    public Context subscreenContext;
    public DexClassLoader dexClassLoader;
    public Class<Object> displayManagerInternal;
    public Constructor[] displayManagerct;
    public Object displayManagerInstance;
    private RelativeLayout.LayoutParams lp1;
    private Matrix matrix;
    private TextureView textureView;
    private VirtualDisplay virtualDisplay;
    private long timeout;
    private static int subscreenSwitch;
    private static Binder sBinder;
    private String inputShell = new StringBuilder("input -d ").append(rearDisplayId.get()).append(" tap 0 0").toString();
    private OrientationEventListener orientationEventListener;

    public static void rearScreenSwitch(boolean z){
        Parcel parcel = Parcel.obtain();
        Parcel parcel2 = Parcel.obtain();
        parcel.writeInterfaceToken("android.os.IPowerManager");
        int code;
        if(z) {
            code = 16777210;
            Binder binder = new Binder();
            sBinder = binder;
            parcel.writeStrongBinder(binder);
            parcel.writeLong(SystemClock.uptimeMillis());
            parcel.writeInt(1);
            parcel.writeString("CAMERA_CALL");
        } else {
            if(subscreenSwitch == 1) {
                code = 16777211;
                parcel.writeLong(SystemClock.uptimeMillis());
                parcel.writeString("CAMERA_CALL");
            } else {
                code = 16777208;
                if (sBinder != null) {
                    parcel.writeStrongBinder(sBinder);
                }
                parcel.writeInt(1);
            }
        }
        try {
            Method serviceMethod = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            IBinder serviceBinder = (IBinder) serviceMethod.invoke(null, "power");
            serviceBinder.transact(code, parcel, parcel2, 1);
        } catch (RemoteException | ClassNotFoundException | NoSuchMethodException e) {
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } finally {
            parcel.recycle();
            parcel2.recycle();
        }
    }

    protected void onCreate(Bundle savedInstanceState){
        orientationEventListener = new OrientationEventListener(getApplicationContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                try {
                    if(Math.round((i + 10) / 90) == Surface.ROTATION_90 && Settings.System.getInt(getContentResolver(), "user_rotation") == Surface.ROTATION_90){
                        screenRotation.set(3);
                    } else if(Math.round((i + 10) / 90) == Surface.ROTATION_180 && Settings.System.getInt(getContentResolver(), "user_rotation") == Surface.ROTATION_180) {
                        screenRotation.set(2);
                    } else if(Math.round((i + 10) / 90) == Surface.ROTATION_270 && Settings.System.getInt(getContentResolver(), "user_rotation") == Surface.ROTATION_270) {
                        screenRotation.set(1);
                    } else if(Math.round((i + 10) / 90) == Surface.ROTATION_0 || Math.round((i + 10) / 90) == 4) {
                        screenRotation.set(Surface.ROTATION_0);
                    }
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            timeout = Settings.System.getLong(getContentResolver(), "subscreen_display_time");
            subscreenSwitch = Settings.System.getInt(getContentResolver(), "subscreen_switch");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT < 33) {
            dexClassLoader = new DexClassLoader("/system/app/MiSubScreenUi/MiSubScreenUi.apk", getCodeCacheDir().getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
        } else {
            dexClassLoader = new DexClassLoader("/system/product/app/MiuiSubScreenUi/MiuiSubScreenUi.apk", getCodeCacheDir().getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
        }
        try {
            subscreenManager = (Class<Object>) dexClassLoader.loadClass("com.xiaomi.misubscreenui.manager.DeviceManager");
            subscreenct = subscreenManager.getDeclaredConstructors();
            subscreenInstance = subscreenct[0].newInstance(getApplicationContext());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor screenDown = sensorManager.getDefaultSensor(33171037);
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                rearScreenSwitch(true);
                //subscreenManager.getMethod("wakeupScreen").invoke(subscreenInstance);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
            sensorManager.registerListener(sensorEventListener, screenDown, 0);
        Window window = getWindow();
        Matrix matrix = new Matrix();
        if (Build.VERSION.SDK_INT < 33) {
        if(screenRotation.get() == 0 || screenRotation.get() == 2) {
                virtualDisplay = mediaProjection.createVirtualDisplay("Mirror",
                        126, 294, 290,
                        displayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        null, null, null);
            } else if (screenRotation.get() == 3 || screenRotation.get() == 1) {
                virtualDisplay = mediaProjection.createVirtualDisplay("Mirror",
                        294, 126, 290,
                        displayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        null, null, null);
            }
        } else {
            virtualDisplay = mediaProjection.createVirtualDisplay("Mirror",
                    294, 294, 290,
                    displayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    null, null, null);
        }
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        window.setAttributes(layoutParams);
        window.setContentView(R.layout.mirror_surface);
        textureView = findViewById(R.id.mirror);
        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                Surface surface = new Surface(surfaceTexture);
                virtualDisplay.setSurface(surface);
                if (screenRotation.get() == 0) {
                    matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                    textureView.setScaleY(1);
                    textureView.setScaleX(-1);
                    if(Build.VERSION.SDK_INT < 33) {
                        matrix.postTranslate(168, 0);
                    } else {
                        matrix.postTranslate(84, 0);
                    }
                } else if (screenRotation.get() == 3) {
                    matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                    textureView.setScaleX(1);
                    textureView.setScaleY(-1);
                    if(Build.VERSION.SDK_INT < 33) {
                        matrix.postTranslate(-168, 0);
                    } else {
                        matrix.postTranslate(-84, 0);
                    }
                } else if (screenRotation.get() == 1) {
                    matrix.setRotate(-90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                    textureView.setScaleY(-1);
                    textureView.setScaleX(1);
                } else if (screenRotation.get() == 2) {
                    matrix.setRotate(-180, textureView.getWidth() / 2, textureView.getHeight() / 2);
                    textureView.setScaleY(-1);
                    textureView.setScaleX(1);
                    if(Build.VERSION.SDK_INT < 33) {
                        matrix.postTranslate(-168, 0);
                    } else {
                        matrix.postTranslate(-84, 0);
                    }
                }
                textureView.setTransform(matrix);
                screenRotation.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable observable, int i) {
                        if (screenRotation.get() == 0) {
                            if (Build.VERSION.SDK_INT < 33) {
                                virtualDisplay.resize(126, 294, 290);
                            }
                            matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleY(1);
                            textureView.setScaleX(-1);
                            if(Build.VERSION.SDK_INT < 33) {
                                matrix.postTranslate(168, 0);
                            } else {
                                matrix.postTranslate(84, 0);
                            }
                        } else if (screenRotation.get() == 3) {
                            if (Build.VERSION.SDK_INT < 33) {
                                virtualDisplay.resize(294, 126, 290);
                            }
                            matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleX(1);
                            textureView.setScaleY(-1);
                            if(Build.VERSION.SDK_INT < 33) {
                                matrix.postTranslate(-168, 0);
                            } else {
                                matrix.postTranslate(-84, 0);
                            }
                        } else if (screenRotation.get() == 1) {
                            if (Build.VERSION.SDK_INT < 33) {
                                virtualDisplay.resize(294, 126, 290);
                            }
                            matrix.setRotate(-90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleY(-1);
                            textureView.setScaleX(1);
                            if(Build.VERSION.SDK_INT >= 33) {
                                matrix.postTranslate(-84, 0);
                            }
                        } else if (screenRotation.get() == 2) {
                            if (Build.VERSION.SDK_INT < 33) {
                                virtualDisplay.resize(126, 294, 290);
                            }
                            matrix.setRotate(-180, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleY(-1);
                            textureView.setScaleX(1);
                            if(Build.VERSION.SDK_INT < 33) {
                                matrix.postTranslate(-168, 0);
                            } else {
                                matrix.postTranslate(-84, 0);
                            }
                        }
                        textureView.setTransform(matrix);
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
        rearScreenSwitch(true);
        orientationEventListener.enable();
        mirroring.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (mirroring.get() == 0) {
                    sensorManager.unregisterListener(sensorEventListener);
                    orientationEventListener.disable();
                    virtualDisplay.release();
                    rearScreenSwitch(false);
                    finishAffinity();
                    System.exit(0);
                }
            }
        });
    }

    public void onPause() {
        super.onPause();

    }

    public void onResume() {
        super.onResume();
        rearScreenSwitch(true);
    }
}
