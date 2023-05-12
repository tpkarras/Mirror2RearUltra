package com.tpkarras.mirror2rearultra;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.databinding.Observable;
import androidx.databinding.ObservableInt;

public class QuickTileService extends TileService {
    public static ObservableInt mirrorSwitch = new ObservableInt(0);
    public static ObservableInt rearDisplayId = new ObservableInt(0);

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        if (tile.getState() == Tile.STATE_INACTIVE) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();
            mirrorSwitch.set(1);
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();
            mirrorSwitch.set(0);
        }
        mirrorSwitch.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                if (mirrorSwitch.get() == 0) {
                    tile.setState(Tile.STATE_INACTIVE);
                    tile.updateTile();
                }
            }
        });
        Intent intent = new Intent(getApplicationContext(), DisplayActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityAndCollapse(intent);
    }
}