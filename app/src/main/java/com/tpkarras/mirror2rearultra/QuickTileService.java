package com.tpkarras.mirror2rearultra;

import static com.tpkarras.mirror2rearultra.DisplayActivity.mirroring;

import android.app.ActivityOptions;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.Display;

import androidx.databinding.ObservableInt;

public class QuickTileService extends TileService {
    public static ObservableInt mirrorSwitch = new ObservableInt(0);
    private ActivityOptions activityOptions;

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        if (tile.getState() == Tile.STATE_INACTIVE) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
            mirrorSwitch.set(1);
        } else {
            mirroring.set(0);
            mirrorSwitch.set(0);
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
        }
        DisplayManager displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        Display[] displays = displayManager.getDisplays();
        ActivityOptions activityOptions = ActivityOptions.makeBasic();
        activityOptions.setLaunchDisplayId(displays[0].getDisplayId());
        Intent intent = new Intent(getApplicationContext(), DisplayActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }
}