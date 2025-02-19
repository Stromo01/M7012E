package com.dji.sdk.sample.demo.flightcontroller;

import android.os.Environment;
import android.util.Log;

import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Logger {
    private File logFile;
    public Logger(){
        File logDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "M7012E");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        logFile = new File(logDir, "ZeroKeyWaypointLog"+System.currentTimeMillis()+".txt");
    }

    public void log(String message) {
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write((message + "\n").getBytes());
        } catch (IOException e) {
            ToastUtils.setResultToToast("Error: " + e);
        }
    }
}
