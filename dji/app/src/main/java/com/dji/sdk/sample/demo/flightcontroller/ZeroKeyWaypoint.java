package com.dji.sdk.sample.demo.flightcontroller;


import static java.lang.Math.abs;

import com.dji.sdk.sample.internal.api.WebserverRequestHandler;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.api.MqttDataStore;

import android.util.Log;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import dji.sdk.flightcontroller.FlightController;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;


public class ZeroKeyWaypoint {
    private float[] current_pos; // Use this to save new coordinates.
    private float current_angle; // Save this as well
    private float[] waypoint_pos;

    private float[] waypoint_angle;
    private FlightController flightController;
    private Context context;

    private List<Waypoints> waypoints = new ArrayList<>();


    private float yaw;
    private float pitch;
    private float throttle;

    private boolean isLookingAtBox;
    private boolean isLookingAtWaypoint;
    private final float waypointAccuracy = 0.1f;//meters
    private final float angleAccuracy= 10f;//degrees
    private final float heightThrottle=0f; //m/s //TODO: Change this to 0.1f
    private final float pitchVelocity=0.1f; //m/s
    private final float yawVelocity=20f; //degress/s
    private static final String TAG = "ZeroKeyWaypoint";
    private Logger logger;

    private WebserverRequestHandler server;

    private Waypoints waypoints_class;

    private JsonHandling jsonHandling;


    public ZeroKeyWaypoint(Context context){
        try {
            this.context = context;
            logger= Logger.getInstance();
            current_pos = new float[]{0, 0, 0};
            server = new WebserverRequestHandler();
            jsonHandling = new JsonHandling();
            server.startMQTTFlow(context);
            //loadWaypointsFromCSV();
        } catch (Exception e) {
            logger.log("Error initializing ZeroKeyWaypoint" + e.getMessage());
        };
    }

    public float[] goToWaypoint(){
        try {
            current_angle = calculateYawFromQuaternion(MqttDataStore.getInstance().getAngle());
            current_pos = MqttDataStore.getInstance().getPosition();
            if (current_pos[0]==0 && current_pos[1]==0 && current_pos[2]==0){
                return new float[]{0,0,0};
            }
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
        logger.log("distance "+distance[0]+", "+distance[1]);
        float height = calculateHeight(current_pos, waypoint_pos);
        logger.log("height "+height);
        if (distance[0] < waypointAccuracy && distance[1] < waypointAccuracy && height < waypointAccuracy){
            logger.log("Arrived at waypoint");

            return true;
        }
        else{
            return false;
        }
    }
    public void nextWaypoint() { //Set next waypoint as current waypoint
        logger.log("nextWaypoint called with " + waypoints.size() + " waypoints");
        waypoints = jsonHandling.setWaypointZeroKey(context);
        logger.log("All the waypoints?"+waypoints);

        if (!waypoints.isEmpty()) {
            waypoint_pos = waypoints.get(0).getnextPos();
            waypoint_angle = waypoints.get(0).getnextAngle();
            waypoints.remove(0);
            isLookingAtWaypoint = false;
            logger.log("Next waypoint: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
            logger.log("Next angle: " + waypoint_angle[0] + ", " + waypoint_angle[1] + ", " + waypoint_angle[2] + ", "+ waypoint_angle[3]);
            ToastUtils.setResultToToast("Next waypoint: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
        }
        else{
            logger.log("No more waypoints");
        }
    }

    private float yawToWaypoint(){
        double angleToWaypoint = calculateAngle(waypoint_pos, current_pos);
        logger.log("AngletoWP: "+angleToWaypoint+"  CurAngle: " +current_angle);
        if (abs(current_angle-angleToWaypoint)<angleAccuracy){//If already at angle
            isLookingAtWaypoint=true;
            logger.log("Yaw to waypoint: Already at angle");
            return 0f;
        }
        else{//Yaw to waypoint//TODO: Check if this is correct
            logger.log("not lloking at wp");
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
                return heightThrottle;
            }
            else{//Drone is above waypoint
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

    private double calculateAngle(float[] waypoint_pos, float[] current_pos) {
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

        double t0 = 2.0 * (w * z + x * y);
        double t1 = 1.0 - 2.0 * (y * y + z * z);

        double result = Math.atan2(t0, t1)*180/3.14;
        logger.log("result angle from quat: "+ result);
        return (float) result;
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

    }

    public ArrayList<float[]> getWaypoints(){
        ArrayList<float[]> waypoints = new ArrayList<>();
        waypoints.add(new float[]{1, 2, 3}); // Add arrays properly
        return waypoints;
    }
    public boolean isLookingAtBox(){
        return isLookingAtBox;
    }
    public void setCurrentPos(float[] current_pos) { //TODO: Remove this
        this.current_pos = current_pos;
    }
    public void setWaypoint(float[] waypoint_pos) {
        waypoints.add(new Waypoints());
    }

}
