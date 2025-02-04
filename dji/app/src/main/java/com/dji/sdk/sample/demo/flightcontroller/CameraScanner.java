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

public class CameraScanner {

    private Camera camera;
    private MediaManager mediaManager;

    public CameraScanner() {
        initializeCamera();
    }

    private void initializeCamera() {
        if (ModuleVerificationUtil.isCameraModuleAvailable()) {
            camera = DJISampleApplication.getAircraftInstance().getCamera();
            if (ModuleVerificationUtil.isMatrice300RTK() || ModuleVerificationUtil.isMavicAir2()) {
                camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError -> ToastUtils.setResultToToast("setFlatMode to PHOTO_SINGLE"));
            } else {
                camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, djiError -> ToastUtils.setResultToToast("setMode to shoot_PHOTO"));
            }
            camera.setMediaFileCallback(new MediaFile.Callback() {
                @Override
                public void onNewFile(@NonNull MediaFile mediaFile) {
                    ToastUtils.setResultToToast("New photo generated");
                }
            });
        }
        mediaManager = camera.getMediaManager();
    }

    public void scanQRCode(final QRCodeScanCallback callback) {
        DJISampleApplication.getProductInstance().getCamera().startShootPhoto(djiError -> {
            if (null == djiError) {
                Log.d("CameraScanner", "Photo taken successfully");
            } else {
                Log.e("CameraScanner", "Error taking photo: " + djiError.getDescription());
            }
        });

        if (mediaManager != null) {
            mediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) {
                        List<MediaFile> mediaFiles = mediaManager.getInternalStorageFileListSnapshot();
                        if (mediaFiles != null && !mediaFiles.isEmpty()) {
                            MediaFile latestMediaFile = mediaFiles.get(0);
                            fetchThumbnailAndDecode(latestMediaFile, callback);
                        } else {
                            Log.e("CameraScanner", "No media files found");
                            callback.onQRCodeScanResult("null row 89");
                        }
                    } else {
                        Log.e("CameraScanner", "Error refreshing file list: " + djiError.getDescription());
                        callback.onQRCodeScanResult("null row 93");
                    }
                }
            });
        } else {
            Log.e("CameraScanner", "MediaManager is null");
            callback.onQRCodeScanResult("null row 99");
        }
    }

    private void fetchThumbnailAndDecode(final MediaFile mediaFile, final QRCodeScanCallback callback) {
        mediaFile.fetchPreview(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError == null) {
                    Bitmap bitmap = mediaFile.getPreview();
                    if (bitmap != null) {
                        String result = decodeQRCode(bitmap);
                        callback.onQRCodeScanResult(result);
                    } else {
                        Log.e("CameraScanner", "Bitmap is null");
                        callback.onQRCodeScanResult("null row 114");
                    }
                } else {
                    Log.e("CameraScanner", "Error fetching preview: " + djiError.getDescription());
                    callback.onQRCodeScanResult("null row 118");
                }
            }
        });
    }



    private void logToFile(String message) {
        try {
            FileWriter writer = new FileWriter(getContext().getExternalFilesDir(null) + "/log.txt", true);
            writer.append(message).append("\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Example usage in your decodeQRCode method
    private String decodeQRCode(Bitmap bitmap) {
        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        RGBLuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = new MultiFormatReader().decode(binaryBitmap);
            return result.getText();
        } catch (Exception e) {
            String errorMessage = "Error decoding QR code: " + e.getMessage();
            Log.e("CameraScanner", errorMessage);
            logToFile(errorMessage);
            logToFile("Bitmap width: " + bitmap.getWidth() + ", height: " + bitmap.getHeight());
            logToFile("BinaryBitmap: " + binaryBitmap.toString());
            return "null row 159";
        }
    }

    public interface QRCodeScanCallback {
        void onQRCodeScanResult(String result);
    }
}