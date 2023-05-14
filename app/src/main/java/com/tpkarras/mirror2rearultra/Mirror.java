package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.DisplayActivity.displayManager;
import static com.tpkarras.mirror2rearultra.DisplayActivity.mediaProjection;
import static com.tpkarras.mirror2rearultra.ForegroundService.screenRotation;
import static com.tpkarras.mirror2rearultra.QuickTileService.mirrorSwitch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.VirtualDisplay;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Mirror extends Activity {
    private IWindowManager wm;
    private TextureView textureView;
    private VirtualDisplay virtualDisplay;
    private static int subscreenSwitch;
    private static Binder sBinder;
    private static Method serviceMethod;
    private IBinder windowBinder;
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
        try {
            serviceMethod = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            windowBinder = (IBinder) serviceMethod.invoke(null, "window");
            wm = IWindowManager.Stub.asInterface(windowBinder);
            Log.d("wm", wm.toString());
            Log.d("wm", String.valueOf(wm.getDefaultDisplayRotation()));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        orientationEventListener = new OrientationEventListener(getApplicationContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int i) {
                try {
                    if(wm.getDefaultDisplayRotation() == 3 && screenRotation.get() != 3){
                        screenRotation.set(3);
                    } else if(wm.getDefaultDisplayRotation() == 2 && screenRotation.get() != 2) {
                        screenRotation.set(2);
                    } else if(wm.getDefaultDisplayRotation() == 1 && screenRotation.get() != 1) {
                        screenRotation.set(1);
                    } else if(wm.getDefaultDisplayRotation() == 0 && screenRotation.get() != 0) {
                        screenRotation.set(Surface.ROTATION_0);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            subscreenSwitch = Settings.System.getInt(getContentResolver(), "subscreen_switch");
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        super.onCreate(savedInstanceState);
        SensorManager sensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor screenDown = sensorManager.getDefaultSensor(33171037);
        SensorEventListener sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                rearScreenSwitch(true);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
            sensorManager.registerListener(sensorEventListener, screenDown, 0);
        Window window = getWindow();
        Matrix matrix = new Matrix();
        virtualDisplay = mediaProjection.createVirtualDisplay("Mirror", 294, 294, 290, displayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, null);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        window.setAttributes(layoutParams);
        window.setContentView(R.layout.mirror_surface);
        textureView = findViewById(R.id.mirror);
        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                Surface surface = new Surface(surfaceTexture);
                virtualDisplay.setSurface(surface);
                try {
                    if (wm.getDefaultDisplayRotation() == 0) {
                        matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        textureView.setScaleX(-1);
                        textureView.setScaleY(1);
                        matrix.postTranslate(84, 0);
                        screenRotation.set(Surface.ROTATION_0);
                    } else if (wm.getDefaultDisplayRotation() == 3) {
                        matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        textureView.setScaleX(1);
                        textureView.setScaleY(-1);
                        matrix.postTranslate(-84, 0);
                        screenRotation.set(3);
                    } else if (wm.getDefaultDisplayRotation() == 1) {
                        matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        textureView.setScaleX(1);
                        textureView.setScaleY(-1);
                        matrix.postTranslate(-84, 0);
                        screenRotation.set(1);
                    } else if (wm.getDefaultDisplayRotation() == 2) {
                        matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        textureView.setScaleX(-1);
                        textureView.setScaleY(1);
                        matrix.postTranslate(84, 0);
                        screenRotation.set(2);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                textureView.setTransform(matrix);
                screenRotation.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable observable, int i) {
                        if (screenRotation.get() == 0) {
                            matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleX(-1);
                            textureView.setScaleY(1);
                            matrix.postTranslate(84, 0);
                        } else if (screenRotation.get() == 3) {
                            matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleX(1);
                            textureView.setScaleY(-1);
                            matrix.postTranslate(-84, 0);
                        } else if (screenRotation.get() == 1) {
                            matrix.setRotate(-90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleX(1);
                            textureView.setScaleY(-1);
                            matrix.postTranslate(-84, 0);
                        } else if (screenRotation.get() == 2) {
                            matrix.setRotate(-180, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            textureView.setScaleX(-1);
                            textureView.setScaleY(1);
                            matrix.postTranslate(84, 0);
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
                sensorManager.unregisterListener(sensorEventListener);
                orientationEventListener.disable();
                virtualDisplay.release();
                if(subscreenSwitch == 1) {
                    Intent intent = new Intent();
                    intent.setComponent(ComponentName.createRelative("com.xiaomi.misubscreenui", ".SubScreenMainActivity"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        };
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        rearScreenSwitch(true);
        orientationEventListener.enable();
        mirrorSwitch.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (mirrorSwitch.get() == 0) {
                    rearScreenSwitch(false);
                    finishAffinity();
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
