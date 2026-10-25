package com.m4dm4x100.fiveswitch;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int REQUEST_CODE = 4105;
    private TextView status;

    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        if (requestCode == REQUEST_CODE) {
            updateStatus();
            Toast.makeText(this, grantResult == PackageManager.PERMISSION_GRANTED
                    ? "Shizuku permission granted" : "Shizuku permission denied", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Shizuku.addRequestPermissionResultListener(permissionListener);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 28);
        root.setGravity(Gravity.TOP);
        root.setBackgroundColor(Color.rgb(250, 250, 250));

        TextView title = new TextView(this);
        title.setText("5G Switch");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(20, 30, 45));
        root.addView(title, matchWrap());

        TextView description = new TextView(this);
        description.setText("\nQuick Settings tile behavior:\n• Selected: request NR-only (5G only)\n• Unselected: request LTE-only (4G only)\n\nThis needs Shizuku running with Wireless debugging, or root. Android does not allow an ordinary app to change modem network types directly.");
        description.setTextSize(16);
        description.setTextColor(Color.DKGRAY);
        root.addView(description, matchWrap());

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, 24, 0, 18);
        root.addView(status, matchWrap());

        Button authorize = new Button(this);
        authorize.setText("Authorize Shizuku");
        authorize.setOnClickListener(v -> requestShizuku());
        root.addView(authorize, matchWrap());

        Button tileHelp = new Button(this);
        tileHelp.setText("How to add Quick Settings tile");
        tileHelp.setOnClickListener(v -> Toast.makeText(this,
                "Pull down Quick Settings twice, tap Edit, then add “5G / LTE”.", Toast.LENGTH_LONG).show());
        root.addView(tileHelp, matchWrap());

        setContentView(root);
        updateStatus();
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void requestShizuku() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Start Shizuku first. Pair it using Wireless debugging.", Toast.LENGTH_LONG).show();
            return;
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Shizuku is already authorized", Toast.LENGTH_SHORT).show();
            updateStatus();
        } else {
            Shizuku.requestPermission(REQUEST_CODE);
        }
    }

    private void updateStatus() {
        if (status == null) return;
        if (!Shizuku.pingBinder()) {
            status.setText("Status: Shizuku is not running. Start it using Wireless debugging.");
        } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            status.setText("Status: Shizuku connected and authorized. Add the tile, then tap it to switch modes.");
        } else {
            status.setText("Status: Shizuku is running, but this app is not authorized yet.");
        }
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }
}
