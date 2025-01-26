package com.dji.sdk.sample.demo.flightcontroller;


import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;
import android.content.Context;


public class ZeroKeyWaypoint {
    private int[] current_pos;
    private int current_angle;
    private int[] waypoint_pos;
    private FlightController flightController;
    private Context context;

    private ArrayList<int[]> waypoints = new ArrayList<int[]>();

    private float yaw;
    private float pitch;
    private float throttle;

    private final float waypointAccuracy = 0.1f;//meters
    private final float heightThrottle=0.2f; //m/s
    private final float pitchVelocity=0.2f; //m/s
    private final float yawVelocity=10f; //degress/s
    private static final String TAG = "ZeroKeyWaypoint";
    private File logFile;

    public ZeroKeyWaypoint(Context context){
        try {
        this.context = context;
        File logDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "M7012E");
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        logFile = new File(logDir, "ZeroKeyWaypointLog"+System.currentTimeMillis()+".txt");
        logToFile("ZeroKeyWaypoint constructor called");
        current_pos = new int[]{0, 0, 0};//TODO: Remove this temporary position for test

            flightController = DJISampleApplication.getAircraftInstance().getFlightController();
            flightController.setYawControlMode(dji.common.flightcontroller.virtualstick.YawControlMode.ANGULAR_VELOCITY);
            flightController.setRollPitchControlMode(dji.common.flightcontroller.virtualstick.RollPitchControlMode.VELOCITY);
            flightController.setVerticalControlMode(dji.common.flightcontroller.virtualstick.VerticalControlMode.VELOCITY);
            loadWaypointsFromCSV("waypoints.csv");
            setWaypoint(new int[]{0, 0, 0}); // Add temp waypoint as first
            nextWaypoint();
        } catch (Exception e) {
            logToFile("Error initializing ZeroKeyWaypoint" + e.getMessage());
            Log.e(TAG, "Error initializing ZeroKeyWaypoint", e);
        };
    }
    public boolean haveArrived(){
        int[] distance = calculateDistance(current_pos, waypoint_pos);
        logToFile("calculateDistance "+distance[0]+", "+distance[1]);
        int height = calculateHeight(current_pos, waypoint_pos);
        logToFile("calculateHeight "+height);
        if (distance[0] < waypointAccuracy && distance[1] < waypointAccuracy && height < waypointAccuracy){
            logToFile("Arrived at waypoint");
            return true;
        }
        else{
            logToFile("Not arrived at waypoint");
            return false;
        }
    }
    public boolean nextWaypoint() {
        logToFile("nextWaypoint called with " + waypoints.size() + " waypoints");
        if (waypoints.size() > 1) {
            waypoint_pos = waypoints.remove(0);
            waypoint_pos = waypoints.get(0);
            logToFile("Next waypoint set: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
            ToastUtils.setResultToToast("Next waypoint set: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
            return true;
        }
        else{
            logToFile("No more waypoints");
            return false;
        }
    }
    public void setCurrentPos(int[] current_pos) { //TODO: Remove this
        this.current_pos = current_pos;
    }
    public void setWaypoint(int[] waypoint_pos) {
        waypoints.add(waypoint_pos);
    }
    public float[] goToWaypoint(){
        try {
            ToastUtils.setResultToToast("Going to waypoint");
            //current_angle = zeroKey.getAngle() //TODO: Set current angle to zeroKey angle
            //current_pos = zeroKey.getPos();//TODO: Set current position to zeroKey position
            //logToFile("goToWaypoint called");
            //int[] distance = calculateDistance(current_pos, waypoint_pos);
            //logToFile("calculateDistance called");
            //int height = calculateHeight(current_pos, waypoint_pos);
            yaw = yawToWaypoint();//Yaw movement TODO: This might need to run in a loop before the other movements
            throttle = 0f;
            pitch = 0f;
            //throttle = throttleToWaypoint(height);//Vertical movement
            //pitch = pitchToWaypoint(distance);//Forward movement
            //yawToBox();
            return new float[]{pitch, throttle, yaw};

        }
        catch (Exception e){
            logToFile("Error in goToWaypoint: " + e.getMessage());
            Log.e(TAG, "Error in goToWaypoint", e);
        }
        return new float[]{pitch, throttle, yaw};
    }


    private float yawToWaypoint(){
        double angleToWaypoint = calculateAngle(current_angle, waypoint_pos, current_pos);
        if (current_angle==angleToWaypoint){//If already at angle
            logToFile("Yaw to waypoint: Already at angle");
            return 0f;
        }
        else{//Yaw to waypoint//TODO: Check if this is correct
            if (angleToWaypoint > current_angle) {
                return yawVelocity; // Yaw right
            } else {
                return -yawVelocity; // Yaw left
            }
        }
    }

    private float throttleToWaypoint(int height){
        if(height>waypointAccuracy){//If height is not the same
            if(height>0){//Drone is below waypoint
                Toast.makeText(context, "Throttle to waypoint: Ascending", Toast.LENGTH_SHORT).show();
                return heightThrottle;
            }
            else{//Drone is above waypoint
                Toast.makeText(context, "Throttle to waypoint: Descending", Toast.LENGTH_SHORT).show();
                return -heightThrottle;
            }
        }
        else{
            return 0f;//Stop moving up or down
        }
    }

    private float pitchToWaypoint(int[] distance){
        if(distance[0]>waypointAccuracy && distance[1]>waypointAccuracy){//If is not in the waypoint area
            return pitchVelocity;
        }
        else{
            return 0f;
        }
    }

    private void yawToBox(){
        //TODO: Implement yaw to box
    }

    private double calculateAngle(int current_angle, int[] waypoint_pos, int[] current_pos) {
        int deltaX = waypoint_pos[0] - current_pos[0];
        int deltaY = waypoint_pos[1] - current_pos[1];
        double angleInRadians = Math.atan2(deltaY, deltaX);
        double angleInDegrees = Math.toDegrees(angleInRadians);
        return angleInDegrees; //TODO:: Add this to current angle
    }

    private int[] calculateDistance(int[] current_pos, int[] waypoint_pos) {
        int [] distance = new int[2];
        for (int i = 0; i < 1; i++) {
            distance[i] = waypoint_pos[i] - current_pos[i];

        }
        return distance;
    }
    private int calculateHeight(int[] current_pos, int[] waypoint_pos) {
        return waypoint_pos[2] - current_pos[2];
    }

    private void loadWaypointsFromCSV(String filePath) {
        /* //TODO: THIS DOESNT WORK
        logToFile("loadWaypointsFromCSV called");
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            logToFile("Reading waypoints from file");
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                int[] pos = new int[3];
                pos[0] = Integer.parseInt(values[0]);
                pos[1] = Integer.parseInt(values[1]);
                pos[2] = Integer.parseInt(values[2]);
                logToFile("Waypoint loaded: " + pos[0] + ", " + pos[1] + ", " + pos[2]);
                setWaypoint(pos);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        for (int i = 0; i < 3; i++){
            int[] pos = new int[3];
            pos[0] = i*10;
            pos[1] = i*10;
            pos[2] = i*10;
            setWaypoint(pos);
        }
    }
    public void logToFile(String message) {
        try (FileOutputStream fos = new FileOutputStream(logFile, true)) {
            fos.write((message + "\n").getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error writing to log file", e);
        }
    }
    public ArrayList<int[]> getWaypoints(){
        return waypoints;
    }


}
