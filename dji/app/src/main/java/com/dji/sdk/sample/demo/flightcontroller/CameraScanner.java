package com.dji.sdk.sample.demo.flightcontroller;

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
            /*camera.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        // Handle error
                    }
                }
            });*/
            mediaManager = camera.getMediaManager();

    }
    private boolean isModuleAvailable() {
        return (null != DJISampleApplication.getProductInstance()) && (null != DJISampleApplication.getProductInstance()
                .getCamera());
    }
    public void scanQRCode(final QRCodeScanCallback callback) {
        DJISampleApplication.getProductInstance().getCamera().startShootPhoto(djiError -> {
                    if (null == djiError) {

                    } else {
                    }
                });
        if (mediaManager != null) {
            mediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError == null) { // we get an error here for image 1 and 2 and 4
                        List<MediaFile> mediaFiles = mediaManager.getInternalStorageFileListSnapshot();
                        if (mediaFiles != null && !mediaFiles.isEmpty()) {
                            MediaFile latestMediaFile = mediaFiles.get(0);
                            fetchThumbnailAndDecode(latestMediaFile, callback);
                        } else {
                            // check if the code gets here
                            callback.onQRCodeScanResult("null row 102"); // image 3
                        }
                    } else {
                        // check if the code gets here
                        callback.onQRCodeScanResult("null row 106"); // image 1 and 2 and 4
                    }
                }
            });
        } else {
            // check if the code gets here
            callback.onQRCodeScanResult("null row 112");
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
                    // check if the code gets here
                    callback.onQRCodeScanResult("null row 126");
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