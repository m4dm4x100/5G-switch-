package com.m4dm4x100.fiveswitch;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import rikka.shizuku.Shizuku;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class NetworkModeTileService extends TileService {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override public void onCreate() {
        super.onCreate();
        DiagnosticLog.add(this, "TileService onCreate");
    }

    @Override public void onStartListening() {
        super.onStartListening();
        DiagnosticLog.add(this, "TileService onStartListening");
        refreshTile();
    }

    @Override public void onClick() {
        super.onClick();
        DiagnosticLog.add(this, "TILE TAP RECEIVED");
        Toast.makeText(this, "5G Switch: tile tap received", Toast.LENGTH_SHORT).show();

        boolean nextNrOnly = !getPrefs().getBoolean("nr_only", false);
        boolean connected = Shizuku.pingBinder();
        boolean authorized = connected && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        DiagnosticLog.add(this, "Shizuku connected=" + connected + ", authorized=" + authorized);

        if (!authorized) {
            Toast.makeText(this, "Open 5G Switch and authorize Shizuku first", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
            return;
        }

        // Keep existing requested bitmasks unchanged for this diagnostic build.
        String bitmask = nextNrOnly ? "10000000000000000000" : "01000001000000000000";
        String command = "cmd phone set-allowed-network-types-for-users 1 " + bitmask
                + " 2>&1";
        String verifyCommand = "cmd phone get-allowed-network-types-for-users -s 0 2>&1";
        String fullCommand = command + " && " + verifyCommand;
        DiagnosticLog.add(this, "Starting shell command; target=" + (nextNrOnly ? "NR-only" : "LTE-only")
                + ", subscription argument=1, verify subscription=0, bitmask=" + bitmask);

        new Thread(() -> {
            Process process = null;
            try {
                Method newProcess = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                newProcess.setAccessible(true);
                process = (Process) newProcess.invoke(null, new String[]{"sh", "-c", fullCommand}, null, null);
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroy();
                    DiagnosticLog.add(this, "COMMAND TIMEOUT after 10 seconds");
                    showToast("5G Switch: shell command timed out");
                    return;
                }
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (output.length() < 4000) output.append(line).append('\n');
                    }
                }
                int exit = process.exitValue();
                String result = output.toString().trim();
                DiagnosticLog.add(this, "Command exit=" + exit + ", output=" + (result.isEmpty() ? "<empty>" : result));
                if (exit == 0 && result.toLowerCase(java.util.Locale.ROOT).contains("completed")) {
                    getPrefs().edit().putBoolean("nr_only", nextNrOnly).apply();
                    mainHandler.post(this::refreshTile);
                    showToast((nextNrOnly ? "NR-only" : "LTE-only") + " request returned success; see diagnostic report");
                } else {
                    showToast("5G Switch command failed; open app and copy diagnostics");
                }
            } catch (Exception e) {
                DiagnosticLog.add(this, "EXCEPTION " + e.getClass().getName() + ": " + String.valueOf(e.getMessage()));
                showToast("5G Switch error: " + e.getClass().getSimpleName() + "; copy diagnostics");
            } finally {
                if (process != null) process.destroy();
            }
        }, "FiveSwitch-Diagnostics").start();
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    private SharedPreferences getPrefs() {
        return getSharedPreferences("network_mode", MODE_PRIVATE);
    }

    private void refreshTile() {
        Tile tile = getQsTile();
        if (tile == null) {
            DiagnosticLog.add(this, "refreshTile: getQsTile returned null");
            return;
        }
        boolean nrOnly = getPrefs().getBoolean("nr_only", false);
        tile.setState(nrOnly ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(nrOnly ? "NR only" : "LTE only");
        tile.setSubtitle(nrOnly ? "5G-only request" : "4G-only request");
        tile.updateTile();
        DiagnosticLog.add(this, "Tile refreshed from saved preference: " + (nrOnly ? "NR-only" : "LTE-only")
                + " (not modem-confirmed)");
    }
}
