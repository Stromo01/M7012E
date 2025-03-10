package com.dji.sdk.sample.demo.flightcontroller;

import android.app.Service;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.util.Log;

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.OnScreenJoystickListener;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.OnScreenJoystick;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;
import com.dji.sdk.sample.internal.api.WebserverRequestHandler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.error.DJIError;
import dji.common.flightcontroller.simulator.InitializationData;
import dji.common.flightcontroller.simulator.SimulatorState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.flightcontroller.Simulator;
import dji.common.flightcontroller.FlightControllerState;
import com.dji.sdk.sample.internal.api.WebserverRequestHandler;



/**
 * Class for virtual stick.
 */
public class VirtualStickView extends RelativeLayout implements CameraScanner.QRCodeScanCallback, View.OnClickListener, CompoundButton.OnCheckedChangeListener, PresentableView {
    private Button btnEnableVirtualStick;
    private Button btnDisableVirtualStick;
    private Button btnHorizontalCoordinate;
    private Button btnSetYawControlMode;
    private Button btnSetVerticalControlMode;
    private Button btnSetRollPitchControlMode;
    private ToggleButton btnSimulator;
    private Button btnTakeOff;

    private boolean stopHandler = false;
    private boolean isWaitingForCallback = false;
    private TextView textView;

    private Handler handler;
    private Timer sendVirtualStickDataTimer;
    private SendVirtualStickDataTask sendVirtualStickDataTask;

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;
    private boolean isSimulatorActived = false;
    private FlightController flightController = null;
    private Runnable updateValuesRunnable;
    private FlightControllerState flightcontrollerState = null;
    private Simulator simulator = null;

    private ZeroKeyWaypoint zeroKey;

    private Logger logger;

    CameraScanner cameraScanner = new CameraScanner(getContext());

    public VirtualStickView(Context context) {
        super(context);
        init(context);

    }

    @NonNull
    @Override
    public String getHint() {
        return this.getClass().getSimpleName() + ".java";
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setUpListeners();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (null != sendVirtualStickDataTimer) {//TODO: Fix this
            if (sendVirtualStickDataTask != null) {
                sendVirtualStickDataTask.cancel();

            }
            sendVirtualStickDataTimer.cancel();
            sendVirtualStickDataTimer.purge();
            sendVirtualStickDataTimer = null;
            sendVirtualStickDataTask = null;
        }
        tearDownListeners();
        super.onDetachedFromWindow();
    }

    private void init(Context context) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        layoutInflater.inflate(R.layout.view_virtual_stick, this, true);
        initParams();
        initUI();
    }


    private void initParams() {
        // We recommand you use the below settings, a standard american hand style.
        if (flightController == null) {
            if (ModuleVerificationUtil.isFlightControllerAvailable()) {
                flightController = DJISampleApplication.getAircraftInstance().getFlightController();
            }
        }
        flightcontrollerState=flightController.getState();
        flightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
        flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
        flightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);
        // Check if the simulator is activated.
        if (simulator == null) {
            simulator = ModuleVerificationUtil.getSimulator();
        }
        isSimulatorActived = simulator.isSimulatorActive();
        logger = Logger.getInstance();
        zeroKey = new ZeroKeyWaypoint(getContext());
    }

    private void initUI() {
        btnEnableVirtualStick = (Button) findViewById(R.id.btn_enable_virtual_stick);
        btnDisableVirtualStick = (Button) findViewById(R.id.btn_disable_virtual_stick);
        btnHorizontalCoordinate = (Button) findViewById(R.id.btn_horizontal_coordinate);
        btnSetYawControlMode = (Button) findViewById(R.id.btn_yaw_control_mode);
        btnSetVerticalControlMode = (Button) findViewById(R.id.btn_vertical_control_mode);
        btnSetRollPitchControlMode = (Button) findViewById(R.id.btn_roll_pitch_control_mode);
        btnTakeOff = (Button) findViewById(R.id.btn_take_off);

        btnSimulator = (ToggleButton) findViewById(R.id.btn_start_simulator);

        textView = (TextView) findViewById(R.id.textview_simulator);


        btnEnableVirtualStick.setOnClickListener(this);
        btnDisableVirtualStick.setOnClickListener(this);
        btnHorizontalCoordinate.setOnClickListener(this);
        btnSetYawControlMode.setOnClickListener(this);
        btnSetVerticalControlMode.setOnClickListener(this);
        btnSetRollPitchControlMode.setOnClickListener(this);
        btnTakeOff.setOnClickListener(this);
        btnSimulator.setOnCheckedChangeListener(VirtualStickView.this);

        if (isSimulatorActived) {
            btnSimulator.setChecked(true);
            textView.setText("Simulator is On.");
        }
    }

    private void setUpListeners() {

    }

    private void tearDownListeners() {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(null);
        }
    }


    @Override
    public void onClick(View v) {
        FlightController flightController = ModuleVerificationUtil.getFlightController();
        if (flightController == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.btn_enable_virtual_stick:
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVirtualStickAdvancedModeEnabled(true);
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;

            case R.id.btn_disable_virtual_stick:
                flightController.setVirtualStickModeEnabled(false, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                break;

            case R.id.btn_roll_pitch_control_mode: // Processes each image taken.
                //cameraScanner.fetchLatestMedia(cameraScanner.getCallback(), System.currentTimeMillis());
                zeroKey.addNewWaypoint(); // add new waypoint to waypoints.json
                break;

            case R.id.btn_yaw_control_mode: // Takes a image once each time the button is pressed.
                long startTime = System.currentTimeMillis();
                cameraScanner.scanQRCode(new CameraScanner.QRCodeScanCallback() {
                    @Override
                    public void onQRCodeScanResult(String result) {
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        logger.log("QR code scan duration: " + duration + " ms");
                        if (result != null) {
                            logger.log("Qrcode result: " + result);
                        } else {
                            logger.log("Qrcode scan failed");
                        }
                    }
                });
                break;
            case R.id.btn_vertical_control_mode:
                zeroKey = new ZeroKeyWaypoint(getContext());
                try{
                    zeroKey.setCurrentPos(zeroKey.getWaypoints().get(0));
                    zeroKey.nextWaypoint();
                }
                catch(Exception e){
                    logger.log("No Zerokey object found");
                    ToastUtils.setResultToToast("No Zerokey object found");
                }

                break;
            case R.id.btn_horizontal_coordinate:
                flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                if(flightcontrollerState.isLandingConfirmationNeeded()){
                    flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            DialogUtils.showDialogBasedOnError(getContext(), djiError);
                        }
                    });
                }
                break;
            case R.id.btn_take_off: //Start waypoint navigation
                flightController.setYawControlMode(dji.common.flightcontroller.virtualstick.YawControlMode.ANGULAR_VELOCITY);
                flightController.setRollPitchControlMode(dji.common.flightcontroller.virtualstick.RollPitchControlMode.VELOCITY);
                flightController.setVerticalControlMode(dji.common.flightcontroller.virtualstick.VerticalControlMode.VELOCITY);


                zeroKey.nextWaypoint();
                logger.log("zerokey done in btn");
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() { // Take off
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError == null) {
                            startWaypointNavigation();
                        }
                        else{
                            DialogUtils.showDialogBasedOnError(getContext(), djiError);
                            logger.log("takeofferror" + djiError.toString());
                        }
                    }
                });
                break;
            default:
                break;
        }

    }

    private void enableVirtualStickMode(FlightController flightController) {
        flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() { // Enable virtual stick mode
            @Override
            public void onResult(DJIError djiError) {
                flightController.setVirtualStickAdvancedModeEnabled(true);
                logger.log("virtual stick error" + djiError.toString());
                if (djiError == null) {
                    startWaypointNavigation();
                }
            }
        });
    }

    private void startWaypointNavigation() {
        pitch = 0f;
        throttle = 0f;
        yaw = 0f;
        if (null == sendVirtualStickDataTimer) { // Create timer and task to send virtual stick data
            sendVirtualStickDataTask = new SendVirtualStickDataTask();
            sendVirtualStickDataTimer = new Timer();
        }
        try {
            startHandler();
        } catch (Exception e) {
            ToastUtils.setResultToToast("Error in takeoff: " + e.getMessage());
            logger.log("Error in takeoff: " + e.getMessage());
        }
    }
    private void startHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        if (updateValuesRunnable == null) {
            updateValuesRunnable = new Runnable() {
                @Override
                public void run() {
                    handleWaypointNavigation();
                    if (!stopHandler) {
                        handler.postDelayed(this, 100);
                    }
                }
            };
        }
        handler.post(updateValuesRunnable);
        logger.log("handler started");
    }

    private void stopHandler() {
        if (handler != null && updateValuesRunnable != null) {
            handler.removeCallbacks(updateValuesRunnable);
            logger.log("handler stopped");
        }
    }
    private void handleWaypointNavigation() {
        try {
            if (!zeroKey.haveArrived()) { // Not at waypoint, go to waypoint
                float[] values = zeroKey.goToWaypoint();
                updateFlightControlData(values);
            } else if (!zeroKey.isLookingAtBox()) { // At waypoint, but not looking at box, yaw to box
                handleYawToBox();
            } else if (zeroKey.isLookingAtBox()) { // At waypoint and looking at box, take picture and scan QR code, go to next waypoint
                logger.log("is looking at box");
                updateFlightControlData(new float[]{0,0,0});
                stopHandler();
                cameraScanner.scanQRCode(new CameraScanner.QRCodeScanCallback() {
                    @Override
                    public void onQRCodeScanResult(String result) {
                        if (result != null) {
                            logger.log("Qrcode result: " + result);
                        } else {
                            logger.log("Qrcode scan failed");
                        }
                        if(!(zeroKey.nextWaypoint())){
                            flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {
                                    DialogUtils.showDialogBasedOnError(getContext(), djiError);
                                }
                            });
                            if(flightcontrollerState.isLandingConfirmationNeeded()){
                                flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                    @Override
                                    public void onResult(DJIError djiError) {
                                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                                    }
                                });
                            }

                        }
                        startHandler();
                    }
                });
            }
        } catch (Exception e) {
            ToastUtils.setResultToToast("Error in waypoint navigation: " + e.getMessage());
            logger.log("Error in waypoint navigation: " + e.getMessage());
        }
    }

    private void handleYawToBox() {
        yaw = zeroKey.yawToBox();
        pitch = 0f;
        throttle = 0f;
        updateFlightControlData(new float[]{pitch, throttle, yaw});
    }

    private void updateFlightControlData(float[] values) { // Update flight control data and send it to the drone using timer and task
        if (values[0] != roll || values[1] != throttle || values[2] != yaw) {
            roll = values[0];
            throttle = values[1];
            yaw = values[2];
            //ToastUtils.setResultToToast("Roll: " + roll + " Throttle: " + throttle + " Yaw: " + yaw);
            logger.log("Roll: " + roll + " Throttle: " + throttle + " Yaw: " + yaw);
            if (sendVirtualStickDataTimer != null && sendVirtualStickDataTask != null) {
                try {
                    sendVirtualStickDataTask.cancel();
                    sendVirtualStickDataTask = new SendVirtualStickDataTask();
                    sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 0, 200);
                } catch (IllegalStateException e) {
                    ToastUtils.setResultToToast("Error scheduling task: " + e.getMessage());
                    logger.log("Error scheduling task: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == btnSimulator) {
            onClickSimulator(b);
        }
    }

    private void onClickSimulator(boolean isChecked) {

    }

    @Override
    public int getDescription() {
        return R.string.flight_controller_listview_virtual_stick;
    }

    @Override
    public void onQRCodeScanResult(String result) {

    }
    private class SendVirtualStickDataTask extends TimerTask {
        @Override
        public void run() {
            if (flightController != null) {
                //接口写反了，setPitch()应该传入roll值，setRoll()应该传入pitch值
                flightController.sendVirtualStickFlightControlData(new FlightControlData(roll, pitch, yaw, throttle), new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null) {
                            ToastUtils.setResultToToast(djiError.getDescription());
                        }
                    }
                });
            }
        }
    }


}
