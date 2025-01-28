package com.dji.sdk.sample.demo.flightcontroller;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
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

import java.util.List;

public class CameraScanner {

    private Camera camera;
    private MediaManager mediaManager;

    public CameraScanner() {
        initializeCamera();
    }

    private void initializeCamera() {
        camera = DJISampleApplication.getProductInstance().getCamera();
        if (camera != null) {
            camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        // Handle error
                    }
                }
            });
            mediaManager = camera.getMediaManager();
        }
    }

    public void scanQRCode(final QRCodeScanCallback callback) {
        if (mediaManager != null) {
            mediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.SDCARD, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        List<MediaFile> mediaFiles = mediaManager.getSDCardFileListSnapshot();
                        if (mediaFiles != null && !mediaFiles.isEmpty()) {
                            MediaFile latestMediaFile = mediaFiles.get(0);
                            fetchThumbnailAndDecode(latestMediaFile, callback);
                        } else {
                            callback.onQRCodeScanResult(null);
                        }
                    } else {
                        callback.onQRCodeScanResult(null);
                    }
                }
            });
        } else {
            callback.onQRCodeScanResult(null);
        }
    }

    private void fetchThumbnailAndDecode(final MediaFile mediaFile, final QRCodeScanCallback callback) {
        mediaFile.fetchPreview(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    Bitmap bitmap = mediaFile.getPreview();
                    String result = decodeQRCode(bitmap);
                    callback.onQRCodeScanResult(result);
                } else {
                    callback.onQRCodeScanResult(null);
                }
            }
        });
    }

    private String decodeQRCode(Bitmap bitmap) {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            // Handle exception
            return null;
        }
    }

    public interface QRCodeScanCallback {
        void onQRCodeScanResult(String result);
    }
}