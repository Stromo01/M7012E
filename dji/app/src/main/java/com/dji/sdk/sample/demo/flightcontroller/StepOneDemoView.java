package com.dji.sdk.sample.demo.flightcontroller;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;

public class StepOneDemoView extends RelativeLayout implements View.OnClickListener, PresentableView {

    private FlightController flightController;
    private Gimbal gimbal;

    private Camera camera;
    private Button btnTakeOff;
    public StepOneDemoView(Context context) {
        super(context);
        initFlightController();
        initGimbal();
        initCamera();
        initUI(context);
        initParams();
    }

    private void initFlightController() {
        if (flightController == null) {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                flightController = DJISampleApplication.getAircraftInstance().getFlightController();
            }
        }
    }
    private void initGimbal() {
        if (gimbal == null) {
            if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
                gimbal = DJISampleApplication.getAircraftInstance().getGimbal();
            }
        }
    }

    private void initCamera() {
        if (camera == null) {
            if (ModuleVerificationUtil.isCameraModuleAvailable()) {
                camera = DJISampleApplication.getAircraftInstance().getCamera();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (flightController == null || gimbal==null) {
            return;
        }
        switch (view.getId()) {
            case R.id.btn_take_off:
                flightController.startTakeoff(djiError -> DialogUtils.showDialogBasedOnError(getContext(), djiError));
                gimbal.rotate(new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).pitch(0).roll(0).yaw(0).time(2).build(), djiError -> DialogUtils.showDialogBasedOnError(getContext(), djiError));
                camera.setFlatMode(SettingsDefinitions.FlatCameraMode.PHOTO_SINGLE, djiError -> ToastUtils.setResultToToast("setFlatMode to PHOTO_SINGLE"));
                gimbal.rotate(new Rotation.Builder().mode(RotationMode.ABSOLUTE_ANGLE).pitch(50).roll(0).yaw(0).time(2).build(), djiError -> DialogUtils.showDialogBasedOnError(getContext(), djiError));
                flightController.startLanding(djiError -> DialogUtils.showDialogBasedOnError(getContext(), djiError));
                break;
        }
    }
    private void initParams() {
        // We recommand you use the below settings, a standard american hand style.
        if (flightController == null) {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                flightController = DJISampleApplication.getAircraftInstance().getFlightController();
            }
        }
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
    }
    private void initUI(Context context) {
        btnTakeOff = (Button) findViewById(R.id.btn_take_off);
        btnTakeOff.setOnClickListener(this);
    }


    @Override
    public int getDescription() {
        return R.string.flight_controller_subtitle_step_one_demo;
    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }
}