package com.dji.sdk.sample.demo.flightcontroller;


import static java.lang.Math.abs;

import com.dji.sdk.sample.internal.api.WebserverRequestHandler;
import com.dji.sdk.sample.internal.controller.DJISampleApplication;
import com.dji.sdk.sample.internal.utils.ToastUtils;
import com.dji.sdk.sample.internal.api.MqttDataStore;

import android.util.Log;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
    private final float waypointAccuracy = 1f;//meters
    private final float angleAccuracy= 2f;//degrees
    private final float heightThrottle=0f; //m/s //TODO: Change this to 0.1f
    private final float pitchVelocity=0.3f; //m/s
    private final float yawVelocity=20f; //degress/s
    private static final String TAG = "ZeroKeyWaypoint";
    private Logger logger;

    private int addedWaypoints;

    private WebserverRequestHandler server;

    private Waypoints waypoints_class;

    private JsonHandling jsonHandling;


    public ZeroKeyWaypoint(Context context){
        try {
            addedWaypoints = 0;
            this.context = context;
            logger= Logger.getInstance();
            current_pos = new float[]{0, 0, 0};
            server = new WebserverRequestHandler();
            jsonHandling = new JsonHandling();
            server.startMQTTFlow(context);
            waypoints = jsonHandling.setWaypointZeroKey(context);
            flightController = Objects.requireNonNull(DJISampleApplication.getAircraftInstance()).getFlightController();
            //loadWaypointsFromCSV();
        } catch (Exception e) {
            logger.log("Error initializing ZeroKeyWaypoint" + e.getMessage());
        };
    }

    public float[] goToWaypoint(){
        try {
            current_angle = -45+flightController.getCompass().getHeading();//calculateYawFromQuaternion(MqttDataStore.getInstance().getAngle());
            current_angle = ((current_angle + 180) % 360 + 360) % 360 - 180;

            current_pos = MqttDataStore.getInstance().getPosition();
            logger.log("curpos: "+ Arrays.toString(current_pos));
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
                logger.log("THE PITCH "+ pitch);
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
    public boolean nextWaypoint() { //Set next waypoint as current waypoint
        logger.log("nextWaypoint called with " + waypoints.size() + " waypoints");
        logger.log("All the waypoints?"+waypoints);

        if (!waypoints.isEmpty()) {
            waypoint_pos = waypoints.get(0).getnextPos();
            waypoint_angle = waypoints.get(0).getnextAngle();
            waypoints.remove(0);
            isLookingAtWaypoint = false;
            logger.log("Next waypoint: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
            logger.log("Next angle: " + waypoint_angle[0] + ", " + waypoint_angle[1] + ", " + waypoint_angle[2] + ", "+ waypoint_angle[3]);
            ToastUtils.setResultToToast("Next waypoint: " + waypoint_pos[0] + ", " + waypoint_pos[1] + ", " + waypoint_pos[2]);
            return true;
        }
        else{
            logger.log("No more waypoints");
            return false;
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
        else { // Yaw to waypoint
            // Calculate the shortest yaw direction
            double angleDifference = angleToWaypoint - current_angle;

            // Normalize angle difference to (-180, 180] range
            angleDifference = ((angleDifference + 180) % 360 + 360) % 360 - 180;

            // Determine yaw direction
            if (angleDifference > 0) {
                return yawVelocity; // Yaw right
            } else {
                return -yawVelocity; // Yaw left
            }
        }
    }

    private float throttleToWaypoint(float height){
        logger.log("throttleouter");
        if(height>waypointAccuracy){//If height is not the same
            if(height>0){//Drone is below waypoint
                logger.log("throttle");
                return heightThrottle;
            }
            else{//Drone is above waypoint
                logger.log("throttle");
                return -heightThrottle;
            }
        }
        else{
            return 0f;//Stop moving up or down
        }
    }

    private float pitchToWaypoint(float[] distance){
        logger.log("pitchouther");
        if(distance[0]>waypointAccuracy || distance[1]>waypointAccuracy){//If is not in the waypoint area
            logger.log("pitch");
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

    //

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
        for (int i = 0; i < 2; i++) {
            //logger.log("WaypointPos: "+ i + waypoint_pos[i] + current_pos[i]);
            distance[i] = Math.abs(waypoint_pos[i] - current_pos[i]);
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

    public void addNewWaypoint() {
        logger.log("Adding new waypoint");
        List<Waypoints> waypointsList = jsonHandling.setWaypointZeroKey(context);
        logger.log("Waypoints: list size: " + waypointsList.size());
        int lastId = 0;
        String newLine = "[\n" +
                "{\"x\": " + current_pos[0] + ",\n" +
                " \"y\": " + current_pos[1] + ",\n" +
                " \"z\": " + current_pos[2] + ",\n" +
                " \"angle\": " + current_angle + ",\n" +
                " \"id\": " + (lastId + 1 + addedWaypoints) + "\n" +
                "}\n" +
                "]";
        if (!waypointsList.isEmpty()) {
            lastId = waypointsList.get(waypointsList.size() - 1).getId();
             newLine = "},\n" +
                    "{\"x\": " + current_pos[0] + ",\n" +
                    " \"y\": " + current_pos[1] + ",\n" +
                    " \"z\": " + current_pos[2] + ",\n" +
                    " \"angle\": " + current_angle + ",\n" +
                    " \"id\": " + (lastId + 1 + addedWaypoints) + "\n" +
                    "}\n" +
                    "]";
            addedWaypoints++;
        } else {
            logger.log("No waypoints found");

            addedWaypoints++;
        }
        logger.log("Last ID: " + lastId);


        logger.log("Adding waypoint to file: " + newLine);
        try {
            File file = new File(context.getFilesDir(), "waypoints.json");
            if (!file.exists()) {
                file.createNewFile();
                logger.log("Created new file: " + file.getPath());
            }
            logger.log("Removing last 2 lines");
            // Read the file content
            List<String> lines = Files.readAllLines(file.toPath());
            if (!lines.isEmpty()) {
                // Remove the last line twice
                lines.remove(lines.size() - 1);
                lines.remove(lines.size() - 1);
            }

            logger.log("Writing the updated content back to the file");
            // Write the updated content back to the file
            Files.write(file.toPath(), lines);

            logger.log("Appending the new line");
            // Append the new line
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);
            logger.log("Writing new line: " + newLine);
            out.println(newLine);
            out.close();
            logger.log("Successfully added waypoint to file: " + newLine);
        } catch (IOException e) {
            logger.log("Error adding waypoint to file: " + e);
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
