package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.ForegroundService.screenRotation;
import static com.tpkarras.mirror2rearultra.QuickTileService.mirroring;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.PowerManager;
import android.util.Log;
import android.view.Surface;

import android.os.Bundle;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;

public class Mirror extends Activity {

    private Matrix matrix;
    private TextureView textureView;
    private static final int REQUEST_MEDIA_PROJECTION = 1;

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
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "StayAwake");
                wl.acquire();
                screenRotation.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable observable, int i) {
                        Log.d("Orientation", String.valueOf(screenRotation.get()));
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
                    }
                });
                mirroring.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
                    @Override
                    public void onPropertyChanged(Observable sender, int propertyId) {
                        if (mirroring.get() == 0) {
                            wl.release();
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
}
