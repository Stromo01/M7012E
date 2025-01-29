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

import androidx.annotation.NonNull;

import com.dji.sdk.sample.R;
import com.dji.sdk.sample.internal.OnScreenJoystickListener;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ModuleVerificationUtil;
import com.dji.sdk.sample.internal.utils.OnScreenJoystick;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.view.PresentableView;

import java.io.BufferedReader;
import java.io.FileReader;
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


/**
 * Class for virtual stick.
 */
public class VirtualStickView extends RelativeLayout implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, PresentableView {
    private Button btnEnableVirtualStick;
    private Button btnDisableVirtualStick;
    private Button btnHorizontalCoordinate;
    private Button btnSetYawControlMode;
    private Button btnSetVerticalControlMode;
    private Button btnSetRollPitchControlMode;
    private ToggleButton btnSimulator;
    private Button btnTakeOff;

    private TextView textView;

    private OnScreenJoystick screenJoystickRight;
    private OnScreenJoystick screenJoystickLeft;

    private Timer sendVirtualStickDataTimer;
    private SendVirtualStickDataTask sendVirtualStickDataTask;

    private float pitch;
    private float roll;
    private float yaw;
    private float throttle;
    private boolean isSimulatorActived = false;
    private FlightController flightController = null;

    private FlightControllerState flightcontrollerState = null;
    private Simulator simulator = null;

    private ZeroKeyWaypoint zeroKey;

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

        screenJoystickRight = (OnScreenJoystick) findViewById(R.id.directionJoystickRight);
        screenJoystickLeft = (OnScreenJoystick) findViewById(R.id.directionJoystickLeft);

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
        if (simulator != null) {
            simulator.setStateCallback(new SimulatorState.Callback() {
                @Override
                public void onUpdate(@NonNull final SimulatorState simulatorState) {
                    ToastUtils.setResultToText(textView,
                            "Yaw : "
                                    + simulatorState.getYaw()
                                    + ","
                                    + "X : "
                                    + simulatorState.getPositionX()
                                    + "\n"
                                    + "Y : "
                                    + simulatorState.getPositionY()
                                    + ","
                                    + "Z : "
                                    + simulatorState.getPositionZ());
                }
            });
        } else {
            ToastUtils.setResultToToast("Simulator disconnected!");
        }

        screenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                float pitchJoyControlMaxSpeed = 10;
                float rollJoyControlMaxSpeed = 10;

                pitch = pitchJoyControlMaxSpeed * pY;
                roll = rollJoyControlMaxSpeed * pX;

                if (null == sendVirtualStickDataTimer) {
                    sendVirtualStickDataTask = new SendVirtualStickDataTask();
                    sendVirtualStickDataTimer = new Timer();
                    sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 100, 200);
                }
            }
        });

        screenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {

            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                float verticalJoyControlMaxSpeed = 4;
                float yawJoyControlMaxSpeed = 20;

                yaw = yawJoyControlMaxSpeed * pX;
                throttle = verticalJoyControlMaxSpeed * pY;

                if (null == sendVirtualStickDataTimer) {
                    sendVirtualStickDataTask = new SendVirtualStickDataTask();
                    sendVirtualStickDataTimer = new Timer();
                    sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 0, 200);
                }
            }
        });
    }

    private void tearDownListeners() {
        Simulator simulator = ModuleVerificationUtil.getSimulator();
        if (simulator != null) {
            simulator.setStateCallback(null);
        }
        screenJoystickLeft.setJoystickListener(null);
        screenJoystickRight.setJoystickListener(null);
    }
    // CameraScanner code implementation maby ???
    /*
    private CameraScanner cameraScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraScanner = new CameraScanner();
        cameraScanner.scanQRCode(this);
    }

    @Override
    public void onQRCodeScanResult(String result) {
        if (result != null) {
            // Display the result
            Toast.makeText(this, "QR Code Result: " + result, Toast.LENGTH_LONG).show();
        } else {
            // Handle the case where the QR code could not be decoded
            Toast.makeText(this, "Failed to decode QR Code", Toast.LENGTH_LONG).show();
        }
    }
    */

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

            case R.id.btn_roll_pitch_control_mode:
                if (flightController.getRollPitchControlMode() == RollPitchControlMode.VELOCITY) {
                    flightController.setRollPitchControlMode(RollPitchControlMode.ANGLE);
                } else {
                    flightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                }
                ToastUtils.setResultToToast(flightController.getRollPitchControlMode().name());
                break;
            case R.id.btn_yaw_control_mode:

                break;
            case R.id.btn_vertical_control_mode:
                zeroKey.setCurrentPos(zeroKey.getWaypoints().get(0));
                zeroKey.nextWaypoint();
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
            case R.id.btn_take_off:
                flightController.startTakeoff(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                flightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        flightController.setVirtualStickAdvancedModeEnabled(true);
                        DialogUtils.showDialogBasedOnError(getContext(), djiError);
                    }
                });
                pitch=0f;
                throttle=0f;
                yaw=0f;
                if (null == sendVirtualStickDataTimer) {
                    sendVirtualStickDataTask = new SendVirtualStickDataTask();
                    sendVirtualStickDataTimer = new Timer();

                }
                try {
                    Handler handler = new Handler(Looper.getMainLooper());
                    Runnable updateValuesRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (!zeroKey.haveArrived()) {
                                float[] values = zeroKey.goToWaypoint();
                                if (values[0] != pitch || values[1] != throttle || values[2] != yaw) {

                                    pitch = values[0];
                                    throttle = values[1];
                                    yaw = values[2];
                                    ToastUtils.setResultToToast("Pitch: " + pitch + " Throttle: " + throttle + " Yaw: " + yaw);
                                    if (sendVirtualStickDataTimer != null && sendVirtualStickDataTask != null) {
                                        try {
                                            sendVirtualStickDataTask.cancel();
                                            sendVirtualStickDataTask = new SendVirtualStickDataTask();
                                            sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 0, 200);
                                        } catch (IllegalStateException e) {
                                            ToastUtils.setResultToToast("Error scheduling task: " + e.getMessage());
                                            zeroKey.logToFile("Error scheduling task: " + e.getMessage());
                                        }
                                    }
                                }
                                handler.postDelayed(this, 200); // Update values again after 200ms
                            } else {
                                zeroKey.logToFile("Land");
                                if (!zeroKey.nextWaypoint()) {
                                    flightController.startLanding(new CommonCallbacks.CompletionCallback() {
                                        @Override
                                        public void onResult(DJIError djiError) {
                                            DialogUtils.showDialogBasedOnError(getContext(), djiError);
                                        }
                                    });
                                    if (flightcontrollerState.isLandingConfirmationNeeded()) {
                                        flightController.confirmLanding(new CommonCallbacks.CompletionCallback() {
                                            @Override
                                            public void onResult(DJIError djiError) {
                                                DialogUtils.showDialogBasedOnError(getContext(), djiError);
                                            }
                                        });
                                    }
                                } else {
                                    handler.postDelayed(this, 200); // Check next waypoint after 200ms //TODO: Is this correct?
                                }
                            }
                        }
                    };
                    handler.post(updateValuesRunnable);
                }catch (Exception e){
                    ToastUtils.setResultToToast("Error in takeoff: " + e.getMessage());
                    zeroKey.logToFile("Error in takeoff: " + e.getMessage());
                }
                /*
                for (int i = 0; i <zeroKey.getWaypoints().size()-1; i++) {
                    ToastUtils.setResultToToast("Waypoint: " + i);
                    try{
                    while(true){//!zeroKey.haveArrived()){
                        float[] values = zeroKey.goToWaypoint();
                        ToastUtils.setResultToToast("done with goToWaypoint");
                        if (values[0] != pitch || values[1] != throttle || values[2] != yaw){
                            pitch = values[0];
                            throttle = values[1];
                            yaw =values[2];
                            ToastUtils.setResultToToast("Pitch: " + pitch + " Throttle: " + throttle + " Yaw: " + yaw);
                            sendVirtualStickDataTimer.schedule(sendVirtualStickDataTask, 0, 200);
                        }

                    }
                    //Drone has arrived at waypoint
                    //TODO: Add camera functions here
                    */

                    /*
                    }
                    catch (Exception e){
                        ToastUtils.setResultToToast("Error in takeoff: " + e.getMessage());
                    }

                }
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
                }*/
                break;
            default:
                break;
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == btnSimulator) {
            onClickSimulator(b);
        }
    }

    private void onClickSimulator(boolean isChecked) {
        if (simulator == null) {
            return;
        }
        if (isChecked) {
            textView.setVisibility(VISIBLE);
            simulator.start(InitializationData.createInstance(new LocationCoordinate2D(23, 113), 10, 10), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    }
                }
            });
        } else {
            textView.setVisibility(INVISIBLE);
            simulator.stop(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if (djiError != null) {
                        ToastUtils.setResultToToast(djiError.getDescription());
                    }
                }
            });
        }
    }

    @Override
    public int getDescription() {
        return R.string.flight_controller_listview_virtual_stick;
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
