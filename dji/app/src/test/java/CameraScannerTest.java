package com.dji.sdk.sample.demo.flightcontroller.Tests;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import com.dji.sdk.sample.demo.flightcontroller.CameraScanner;

public class CameraScannerTest {

    @Test
    public void testDecodeQRCode() {
        // Path to the PNG file
        String filePath = "frame.png";

        // Load the PNG file into a Bitmap
        Bitmap bitmap = loadBitmapFromFile(filePath);

        assertNotNull("Failed to load the PNG file", bitmap);

        // Decode the QR code
        CameraScanner cameraScanner = new CameraScanner();
        String result = cameraScanner.decodeQRCode(bitmap);

        assertNotNull("Failed to decode QR Code", result);
        System.out.println("QR Code Result: " + result);
    }

    private Bitmap loadBitmapFromFile(String filePath) {
        try {
            File file = new File(filePath);
            FileInputStream fileInputStream = new FileInputStream(file);
            return BitmapFactory.decodeStream(fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
