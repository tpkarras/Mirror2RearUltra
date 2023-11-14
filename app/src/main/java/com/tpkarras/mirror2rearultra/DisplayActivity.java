package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.QuickTileService.mirrorSwitch;
import static com.tpkarras.mirror2rearultra.QuickTileService.rearDisplayId;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.Display;

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
   private ActivityOptions activityOptions;
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
                       mediaProjection = mediaProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                       displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
                       Display[] displays = displayManager.getDisplays();
                       for (Display display : displays) {
                          if(display.getName().equals("Built-in Screen") && display.getDisplayId() != 0) {
                             rearDisplayId.set(display.getDisplayId());
                             break;
                          }
                       }
                       activityOptions = activityOptions.makeBasic();
                       activityOptions.setLaunchDisplayId(rearDisplayId.get());
                       Intent intent = new Intent(getApplicationContext(), Mirror.class);
                       intent.setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT | Intent.FLAG_ACTIVITY_NEW_TASK);
                       startActivity(
                               intent,
                               activityOptions.toBundle()
                       );
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
         Intent foreground = new Intent(getApplicationContext(), ForegroundService.class);
            if (mirrorSwitch.get() == 1) {
               startService(foreground);
               mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
               resultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent());
            } else {
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