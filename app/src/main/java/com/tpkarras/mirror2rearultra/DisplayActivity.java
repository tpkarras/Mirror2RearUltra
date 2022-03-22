package com.tpkarras.mirror2rearultra;
import static com.tpkarras.mirror2rearultra.QuickTileService.mirrorSwitch;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.view.Display;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.ObservableInt;

import android.os.Bundle;

public class DisplayActivity extends AppCompatActivity {

   public static MediaProjection mediaProjection;
   private ActivityOptions activityOptions;
   private Activity activity;
   public static VirtualDisplay virtualDisplay;
   public static DisplayManager displayManager;
   public static MediaProjectionManager mediaProjectionManager;
   public static ObservableInt mirroring = new ObservableInt(0);
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
      ActivityResultLauncher<Intent> resultLauncher = registerForActivityResult(
              new ActivityResultContracts.StartActivityForResult(),
              new ActivityResultCallback<ActivityResult>() {

                 @Override
                 public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() != 0) {
                       mirroring.set(1);
                       mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                       virtualDisplay = mediaProjection.createVirtualDisplay("Mirror",
                               126, 294, 290,
                               displayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR | displayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                               null, null, null);
                       DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                       Display[] displays = displayManager.getDisplays();
                       activityOptions = activityOptions.makeBasic();
                       activityOptions.setLaunchDisplayId(displays[1].getDisplayId());
                       Intent intent = new Intent(getApplicationContext(), Mirror.class);
                       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       startActivity(
                               intent,
                               activityOptions.toBundle()
                       );
                       moveTaskToBack (true);
                    } else {
                       mirrorSwitch.set(0);
                       Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
                       stopService(foreground);
                       moveTaskToBack (true);
                    }
                 }

              });
      if (isAppInstalled(this, "com.xiaomi.misubscreenui")) {
         super.onCreate(savedInstanceState);
               if (mirrorSwitch.get() == 1) {
                  Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
                  startService(foreground);
                  mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                  resultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
               } else {
                  mirroring.set(0);
                  Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
                  stopService(foreground);
                  moveTaskToBack (true);
               }
      } else {
         new AlertDialog.Builder(this)
                 .setMessage("The device you're using is not a Mi 11 Ultra.")
                 .setPositiveButton("OK", null)
                 .create()
                 .show();
      }
   }
}