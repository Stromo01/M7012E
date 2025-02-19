package com.dji.sdk.sample.demo.flightcontroller;


import com.dji.sdk.sample.internal.api.WebserverRequestHandler;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.DialogUtils;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.api.MqttDataStore;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.util.CommonCallbacks;
import dji.sdk.flightcontroller.FlightController;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;


public class ZeroKeyWaypoint {
    private float[] current_pos;
    private float current_angle;
    private float[] waypoint_pos;
    private FlightController flightController;
    private Context context;

    private ArrayList<float[]> waypoints = new ArrayList<float[]>();

    private float yaw;
    private float pitch;
    private float throttle;

    private boolean isLookingAtBox;
    private boolean isLookingAtWaypoint;
    private final float waypointAccuracy = 0.1f;//meters
    private final float heightThrottle=0.1f; //m/s
    private final float pitchVelocity=0.1f; //m/s
    private final float yawVelocity=90f; //degress/s
    private static final String TAG = "ZeroKeyWaypoint";
    private Logger logger;

    private WebserverRequestHandler server;


    public ZeroKeyWaypoint(Context context){
        try {
            this.context = context;
            logger= Logger.getInstance();
            current_pos = new float[]{0, 0, 0};
            server = new WebserverRequestHandler();
            server.startMQTTFlow(context);
            initFlightController();
            loadWaypointsFromCSV();
            nextWaypoint();
        } catch (Exception e) {
            logger.log("Error initializing ZeroKeyWaypoint" + e.getMessage());
        };
    }
    private void initFlightController(){
        flightController = DJISampleApplication.getAircraftInstance().getFlightController();
        flightController.setYawControlMode(dji.common.flightcontroller.virtualstick.YawControlMode.ANGULAR_VELOCITY);
        flightController.setRollPitchControlMode(dji.common.flightcontroller.virtualstick.RollPitchControlMode.VELOCITY);
        flightController.setVerticalControlMode(dji.common.flightcontroller.virtualstick.VerticalControlMode.VELOCITY);
    }
    public float[] goToWaypoint(){
        try {
            current_angle = calculateYawFromQuaternion(MqttDataStore.getInstance().getAngle());
            current_pos = MqttDataStore.getInstance().getPosition();
            float[] distance = calculateDistance(current_pos, waypoint_pos);
            float height = calculateHeight(current_pos, waypoint_pos);
            if (!isLookingAtWaypoint){
                yaw = yawToWaypoint();//Turn the drone to face the waypoint first
            }
            else{
                yaw = yawToWaypoint();//Yaw movement
                throttle = throttleToWaypoint(height);//Vertical movement
                pitch = pitchToWaypoint(distance);//Forward movement
            }
            //yawToBox();
            return new float[]{pitch, throttle, yaw};

        }
        catch (Exception e){
            logger.log("Error in goToWaypoint: " + e.getMessage());
            Log.e(TAG, "Error in goToWaypoint", e);
        }
        return new float[]{pitch, throttle, yaw};
    }

    public boolean haveArrived(){ //Check if drone is within specified accuracy of waypoint
        float[] distance = calculateDistance(current_pos, waypoint_pos);
        logger.log("calculateDistance "+distance[0]+", "+distance[1]);
        float height = calculateHeight(current_pos, waypoint_pos);
        logger.log("calculateHeight "+height);
        if (distance[0] < waypointAccuracy && distance[1] < waypointAccuracy && height < waypointAccuracy){
            logger.log("Arrived at waypoint");
            return true;
        }
        else{
            logger.log("Not arrived at waypoint");
            return false;
        }
    }
    public boolean nextWaypoint() { //Set next waypoint as current waypoint
        logger.log("nextWaypoint called with " + waypoints.size() + " waypoints");
        if (waypoints.size() > 1) {
            waypoint_pos = waypoints.remove(0);
            isLookingAtWaypoint = false;
            logger.log("Next waypoint: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
            ToastUtils.setResultToToast("Next waypoint: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
            return true;
        }
        else{
            logger.log("No more waypoints");
            return false;
        }
    }

    private float yawToWaypoint(){
        double angleToWaypoint = calculateAngle(current_angle, waypoint_pos, current_pos);
        if (current_angle==angleToWaypoint){//If already at angle
            isLookingAtWaypoint=true;
            logger.log("Yaw to waypoint: Already at angle");
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

    private float throttleToWaypoint(float height){
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

    private float pitchToWaypoint(float[] distance){
        if(distance[0]>waypointAccuracy || distance[1]>waypointAccuracy){//If is not in the waypoint area
            return pitchVelocity;
        }
        else{
            return 0f;
        }
    }

    public float yawToBox(){
        float angleToBox = 3f;//calculateYawFromQuaternion(zeroKey.getWaypointAngle());//TODO: Implement this
        isLookingAtBox=false;
        if (current_angle==angleToBox){//If already at angle
            logger.log("Yaw to box: Already at angle");
            isLookingAtBox=true;
            return 0f;
        }
        else{//Yaw to waypoint//TODO: Check if this is correct
            if (angleToBox > current_angle) {
                return yawVelocity; // Yaw right
            } else {
                return -yawVelocity; // Yaw left
            }
        }
    }

    private double calculateAngle(float current_angle, float[] waypoint_pos, float[] current_pos) {
        float deltaX = waypoint_pos[0] - current_pos[0];
        float deltaY = waypoint_pos[1] - current_pos[1];
        double angleInRadians = Math.atan2(deltaY, deltaX);
        double angleInDegrees = Math.toDegrees(angleInRadians);
        return angleInDegrees; //TODO:: Add this to current angle
    }

    private float calculateYawFromQuaternion(float[] quaternion) {
        double w = quaternion[0];
        double x = quaternion[1];
        double y = quaternion[2];
        double z = quaternion[3];

        double t0 = +2.0 * (w * x + y * z);
        double t1 = +1.0 - 2.0 * (x * x + y * y);
        return (float) Math.atan2(t0, t1);
    }

    private float[] calculateDistance(float[] current_pos, float[] waypoint_pos) {
        float [] distance = new float[2];
        for (int i = 0; i < 1; i++) {
            distance[i] = waypoint_pos[i] - current_pos[i];
        }
        return distance;
    }
    private float calculateHeight(float[] current_pos, float[] waypoint_pos) {
        return waypoint_pos[2] - current_pos[2];
    }


    class Item {
        int id;
        float position;
        float angle;

        public Item(int id, float position, float angle) {
            this.id = id;
            this.position = position;
            this.angle = angle;
        }

        @Override
        public String toString() {
            return "ID: " + id + ", Position: " + position + ", Angle: " + angle;
        }
    }
    private void loadWaypointsFromCSV() {
        //TODO: THIS DOESNT WORK
        try{
            String content = new String(Files.readAllBytes(Paths.get("waypoints.json")));
            JSONObject jsonObject = new JSONObject(content);
            JSONArray itemsArray = jsonObject.getJSONArray("items");
            List<Item> itemList = new ArrayList<>();

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemObj = itemsArray.getJSONObject(i);
                int id = itemObj.getInt("id");
                double position_ = itemObj.getDouble("position");
                double angle_ = itemObj.getDouble("angle");
                float position = (float)position_;
                float angle = (float)angle_;

                // Create item object and add to list
                itemList.add(new Item(id, position, angle));
                logger.log("waypoints"+ itemList);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        //setWaypoint(new float[]{3, -1, 2});

    }
    public ArrayList<float[]> getWaypoints(){
        return waypoints;
    }
    public boolean isLookingAtBox(){
        return isLookingAtBox;
    }
    public void setCurrentPos(float[] current_pos) { //TODO: Remove this
        this.current_pos = current_pos;
    }
    public void setWaypoint(float[] waypoint_pos) {
        waypoints.add(waypoint_pos);
    }

}
