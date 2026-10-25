package com.m4dm4x100.fiveswitch;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DiagnosticLog {
    private static final String FILE_NAME = "diagnostics.txt";
    private DiagnosticLog() {}

    public static synchronized void add(Context context, String message) {
        try {
            String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
            String line = stamp + " | " + message.replace('\n', ' ') + "\n";
            try (FileOutputStream out = context.openFileOutput(FILE_NAME, Context.MODE_APPEND)) {
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ignored) { }
    }

    public static synchronized String read(Context context) {
        try {
            File file = new File(context.getFilesDir(), FILE_NAME);
            if (!file.exists()) return "No diagnostic events recorded yet.";
            byte[] bytes = new byte[(int) file.length()];
            try (java.io.FileInputStream in = new java.io.FileInputStream(file)) {
                int offset = 0, count;
                while (offset < bytes.length && (count = in.read(bytes, offset, bytes.length - offset)) > 0) offset += count;
                return new String(bytes, 0, offset, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return "Could not read diagnostics: " + e.getClass().getSimpleName();
        }
    }

    public static synchronized void clear(Context context) {
        try (FileOutputStream out = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE)) {
            out.write(new byte[0]);
        } catch (Exception ignored) { }
    }
}
