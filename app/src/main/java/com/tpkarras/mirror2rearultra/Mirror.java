package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.ForegroundService.screenRotation;
import static com.tpkarras.mirror2rearultra.QuickTileService.mirroring;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import android.os.Bundle;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;

import java.lang.reflect.Constructor;

import dalvik.system.DexClassLoader;

public class Mirror extends Activity {

    private Matrix matrix;
    private TextureView textureView;

    public void subscreenDisplayTrigger(boolean trigger) {
        if (trigger == true) {
            try {
                DexClassLoader dexClassLoader = new DexClassLoader("/system/app/MiSubScreenUi/MiSubScreenUi.apk", getCodeCacheDir().getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
                Class<Object> subscreenManager = (Class<Object>) dexClassLoader.loadClass("com.xiaomi.misubscreenui.manager.DeviceManager");
                Constructor[] subscreenct = subscreenManager.getDeclaredConstructors();
                Object subscreenInstance = subscreenct[0].newInstance(getApplicationContext());
                subscreenManager.getMethod("wakeupScreen").invoke(subscreenInstance);
                subscreenManager.getMethod("mo7860f").invoke(subscreenInstance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (trigger == false){
            try {
                DexClassLoader dexClassLoader = new DexClassLoader("/system/app/MiSubScreenUi/MiSubScreenUi.apk", getCodeCacheDir().getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
                Class<Object> subscreenManager = (Class<Object>) dexClassLoader.loadClass("com.xiaomi.misubscreenui.manager.DeviceManager");
                Constructor[] subscreenct = subscreenManager.getDeclaredConstructors();
                Object subscreenInstance = subscreenct[0].newInstance(getApplicationContext());
                subscreenManager.getMethod( "mo7857c").invoke(subscreenInstance);
                subscreenManager.getMethod("letSubScreenOff").invoke(subscreenInstance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Matrix matrix = new Matrix();
        setContentView(R.layout.mirror_surface);
        textureView = findViewById(R.id.mirror);
        TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                Surface surface = new Surface(surfaceTexture);
                DisplayActivity.virtualDisplay.setSurface(surface);
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
                            DisplayActivity.virtualDisplay.resize(126, 294, 290);
                            matrix.setRotate(0, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        } else if (screenRotation.get() == 3) {
                            DisplayActivity.virtualDisplay.resize(294, 126, 290);
                            matrix.setRotate(90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                            matrix.postTranslate(-167, 0);
                        } else if (screenRotation.get() == 1) {
                            DisplayActivity.virtualDisplay.resize(294, 126, 290);
                            matrix.setRotate(-90, textureView.getWidth() / 2, textureView.getHeight() / 2);
                        } else if (screenRotation.get() == 2) {
                            DisplayActivity.virtualDisplay.resize(126, 294, 290);
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
