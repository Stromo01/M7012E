package com.dji.sdk.sample.demo.flightcontroller;

import static dji.midware.data.manager.P3.ServiceManager.getContext;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.RGBLuminanceSource;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.BaseThreeBtnView;

import androidx.annotation.NonNull;
import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.media.MediaFile;

import java.io.FileWriter;
import java.io.IOException;

import android.os.Handler;
import android.os.Looper;


public class CameraScanner {

    private Camera camera;

    private ZeroKeyWaypoint zeroKey;
    private MediaManager mediaManager;  // Hanterar bilder och videor från kameran
    private  Context context;  // add
    private int i = 0;

    public CameraScanner() { // add
        initializeCamera();  // sätta upp kameran
    }
    // Kollar om kamera är tillgänglig
    private void initializeCamera() {
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            // Hämtar kameran från drönaren och kontrollerar att den inte är null
            camera = DJISampleApplication.getAircraftInstance().getCamera();
            if (camera != null){
                // Om drönaren är en Matrice 300 RTK eller Mavic Air 2, sätts kameran i single photo mode
                if (ModuleVerificationUtil.isMatrice300RTK() || ModuleVerificationUtil.isMavicAir2()) {
                    // Anropar setFlatMode() på camera för att sätta den i "PHOTO_SINGLE"-läge.
                    camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError -> {
                        // kolla om det finns fel
                        if (djiError == null){  // om kameraläget sattes korrekt
                            Log.d("CameraScanner", "Camera mode set to PHOTO_SINGLE"); // loggmeddelande (Log.d) skrivs ut

                        }else { // om det finns fel
                            Log.e("CameraScanner", "Error setting camera mode: " + djiError.getDescription());
                        }
                    });

                }else {
                    // sätts kamera att gå in i enskilt fotoläge
                    camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError -> {
                        if (djiError == null) { // om  kameran har växlats till PHOTO_SINGLE läge utan problem.
                            Log.d("CameraScanner", "Camera mode set to SHOOT_PHOTO"); // logga ett meddelande om att allt gick bra
                        } else { // något gick fel när kameran skulle växlas till PHOTO_SINGLE
                            // skrivs ut ett fel medelande + hämtar en beskrivning av felet
                            Log.e("CameraScanner", "Error setting camera mode: " + djiError.getDescription());
                        }
                    });
                }

                mediaManager = camera.getMediaManager(); // hämta MediaManager från kameran
                if (mediaManager == null) {
                    Log.e("CameraScanner", "Error: MediaManager is null");
                }
            } else {
                Log.e("CameraScanner", "Error: Camera is null");
            }
        } else {
            Log.e("CameraScanner", "Error: Camera module is not available");
        }
    }



    public void scanQRCode(final QRCodeScanCallback callback) {
        if (camera == null || mediaManager == null) {
            Log.e("CameraScanner", "Camera or MediaManager not initialized");
            callback.onQRCodeScanResult("null - Camera or MediaManager not initialized");
            i = 0;
            return;
        }
        camera.startShootPhoto(djiError -> {
            if (djiError == null) {
                Log.d("CameraScanner", "Photo taken successfully");
                fetchLatestMedia(callback);
                i = 0;
            } else {
                if (i < 10) {
                    i++;
                    //initializeCamera();
                    scanQRCode(callback);
                }
                else {
                    callback.onQRCodeScanResult("null - Error taking photo loop : " + i);
                    i = 0;
                    Log.e("CameraScanner", "Error taking photo: " + djiError.getDescription());
                    callback.onQRCodeScanResult("null - Error taking photo");
                }

            }
        });
    }
    // Tar emot ett QRCodeScanCallback objekt för att returnera resultatet av QR-kodsskanningen
    private void fetchLatestMedia(final QRCodeScanCallback callback) {

        mediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    List<MediaFile> mediaFiles = mediaManager.getInternalStorageFileListSnapshot();
                    if (mediaFiles != null && !mediaFiles.isEmpty()) {
                        MediaFile latestMediaFile = mediaFiles.get(mediaFiles.size() - 1);
                        i = 0;
                        fetchThumbnailAndDecode(latestMediaFile, callback);
                    } else {
                        i = 0;
                        Log.e("CameraScanner", "No media files found");
                        callback.onQRCodeScanResult("null - No media files found");
                    }
                } else {
                    if (i <= 20){ // Around 99.97 % chance to work if we take 15 images with 33 % chance for one image to work.
                        i++;
                        fetchLatestMedia(callback); // If this doesn't solve the problem run the entire image process instead. (scanQRCode())
                    } else if (i <= 40) { // Around 99.97 % chance to work if correct.
                        i++;
                        initializeCamera();
                        scanQRCode(callback);
                    } else {
                        i = 0;
                        //Log.e("CameraScanner", "Error refreshing file list: " + djiError.getDescription());
                        zeroKey.logToFile("Error refreshing file list: " + djiError.getDescription());
                        callback.onQRCodeScanResult("null - Error refreshing file list");
                    }
                }
            }
        });
    }

    // MediaFile mediaFile: Den bildfil som ska hämtas och skannas.
    // QRCodeScanCallback callback: Callback som används för att returnera resultatet av QR-kodsskanningen.
    private void fetchThumbnailAndDecode(final MediaFile mediaFile, final QRCodeScanCallback callback) {
        mediaFile.fetchPreview(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    Bitmap bitmap = mediaFile.getPreview();
                    if (bitmap != null && bitmap.getWidth() > 0 && bitmap.getHeight() > 0) {
                        String result = decodeQRCode(bitmap);
                        if (result != null && !result.isEmpty()) {
                            callback.onQRCodeScanResult(result);
                        } else {
                            Log.e("CameraScanner", "Bitmap is null");
                            callback.onQRCodeScanResult("null row 114");
                        }
                    } else {
                        i= 0;
                        Log.e("CameraScanner", "Error fetching preview: " + djiError.getDescription());
                        callback.onQRCodeScanResult("null row 118");
                    }
                } else {
                    i = 0;
                    Log.e("CameraScanner", "Error fetching preview: " + djiError.getDescription());
                    callback.onQRCodeScanResult("null - Error fetching preview: " + djiError.getDescription());
                }
            }
        });
    }

            // Example usage in your decodeQRCode method
            private String decodeQRCode(Bitmap bitmap) {
                int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

                RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    Result result = new MultiFormatReader().decode(binaryBitmap);
                    i = 0;
                    return result.getText();
                } catch (Exception e) {
                    String errorMessage = "Error decoding QR code: " + e.getMessage();
                    Log.e("CameraScanner", "errordecoding" + e.getMessage());
                    zeroKey.logToFile(errorMessage);
                    zeroKey.logToFile("Bitmap width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());
                    zeroKey.logToFile("errordecoding" + e.getMessage());
                    i = 0;
                    return "null row 159";
                }
            }

    public interface QRCodeScanCallback {
        void onQRCodeScanResult(String result);
    }
}
