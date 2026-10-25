package com.m4dm4x100.fiveswitch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class NetworkModeTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        refreshTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean nextNrOnly = !getPrefs().getBoolean("nr_only", false);

        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Open 5G Switch and authorize Shizuku first", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
            return;
        }

        // Android's cmd phone accepts the allowed-network bitmask as a base-2 string.
        // Android network type IDs: LTE=13, NR=20.
        String bitmask = nextNrOnly ? "10000000000000000000" : "1000000000000";
        String command = "cmd phone set-allowed-network-types-for-users -s 0 " + bitmask;

        try {
            Process process = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errors = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append('\n');
                while ((line = errors.readLine()) != null) output.append(line).append('\n');
            }
            int exit = process.waitFor();
            if (exit == 0) {
                getPrefs().edit().putBoolean("nr_only", nextNrOnly).apply();
                refreshTile();
                Toast.makeText(this, nextNrOnly ? "NR-only mode requested" : "LTE-only mode requested", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Change rejected by Android/OEM. " + output.toString().trim(), Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not change network mode: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("network_mode", MODE_PRIVATE);
    }

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean nrOnly = getPrefs().getBoolean("nr_only", false);
        tile.setState(nrOnly ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(nrOnly ? "NR only" : "LTE only");
        tile.setSubtitle(nrOnly ? "5G-only request" : "4G-only request");
        tile.updateTile();
    }
}
