package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.QuickTileService.mirrorSwitch;
import static com.tpkarras.mirror2rearultra.QuickTileService.mirroring;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class DisplayActivity extends AppCompatActivity {

   public static MediaProjection mediaProjection;
   public static ActivityResultLauncher<Intent> resultLauncher;
   public static DisplayManager displayManager;
   public static MediaProjectionManager mediaProjectionManager;
   public static boolean isAppInstalled(Context context, String packageName) {
      try {
         context.getPackageManager().getApplicationInfo(packageName, 0);
         return true;
      } catch (PackageManager.NameNotFoundException e) {
         return false;
      }
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      resultLauncher = registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult(),
              new ActivityResultCallback<ActivityResult>() {
                 @Override
                 public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() != 0) {
                       mirroring.set(1);
                       mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                       displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                       finish();
                    } else {
                       mirrorSwitch.set(0);
                       Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
                       stopService(foreground);
                       finish();
                    }
                 }

              });
      if (isAppInstalled(this, "com.xiaomi.misubscreenui")) {
         super.onCreate(savedInstanceState);
            if (mirrorSwitch.get() == 1) {
               Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
               startService(foreground);
            } else {
               mirroring.set(0);
               Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
               stopService(foreground);
               finish();
            }
      } else {
         new AlertDialog.Builder(this)
                 .setMessage(R.string.incompatible)
                 .setPositiveButton(android.R.string.ok, null)
                 .create()
                 .show();
         mirrorSwitch.set(0);
         finish();
      }
   }
}