package com.dji.sdk.sample.demo.timeline;

import com.dji.sdk.sample.internal.controller.DJISampleApplication;

import java.util.ArrayList;

import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.sdk.flightcontroller.FlightController;

public class ZeroKeyWaypoint {
    private int[] current_pos;
    private int current_angle;
    private int[] waypoint_pos;
    private FlightController flightController;

    private ArrayList<int[]> waypoints = new ArrayList<int[]>();

    private final float waypointAccuracy = 0.1f;//meters

    private final float heightThrottle=0.2f; //m/s
    private final float pitchVelocity=0.2f; //m/s

    private final float yawVelocity=0.2f; //m/s



    private void init(){
        flightController = DJISampleApplication.getAircraftInstance().getFlightController();
        setWaypoint(new int[]{0, 0, 0});//Add temp waypoint as first
    }
    public void setWaypoint(int[] waypoint_pos) {
        waypoints.add(waypoint_pos);
    }
    public void nextWaypoint() {
        if (waypoints.size() > 1) {
            waypoint_pos = waypoints.remove(0);
            waypoint_pos = waypoints.get(0);
            //TODO: Set current angle to zeroKey angle
        }
    }
    public void goToWaypoint(){
        int[] distance = calculateDistance(current_pos, waypoint_pos);
        int height = calculateHeight(current_pos, waypoint_pos);

        FlightControlData data = new FlightControlData(0,0,0,0);
        while(distance[0]>waypointAccuracy && distance[1]>waypointAccuracy && height>waypointAccuracy){
            yawToWaypoint(data);//Yaw movement
            throttleToWaypoint(data,height);//Vertical movement
            pitchToWaypoint(data,distance);//Forward movement
            flightController.sendVirtualStickFlightControlData(data, null);
            //current_pos=zeroKey.getPos();//TODO: Add this
            height = calculateHeight(current_pos, waypoint_pos);
            distance = calculateDistance(current_pos, waypoint_pos);
        }
        yawToBox(data);

    }
    private void yawToWaypoint(FlightControlData data){
        double angleToWaypoint = calculateAngle(current_angle, waypoint_pos, current_pos);
        if (current_angle==angleToWaypoint){//If already at angle
            data.setYaw(0);//Stop yawing
            return;
        }
        else{//Yaw to waypoint
            data.setYaw(yawVelocity);//TODO: Set angle type to velocity (degrees/s)
        }
    }

    private void throttleToWaypoint(FlightControlData data, int height){
        if(height>waypointAccuracy){//If height is not the same
            if(height>0){//Drone is below waypoint
                data.setVerticalThrottle(heightThrottle);//TODO: set type to velocity (m/s)
            }
            else{//Drone is above waypoint
                data.setVerticalThrottle(-heightThrottle);//TODO: set type to velocity (m/s)
            }
        }
        else{
            data.setVerticalThrottle(0);//Stop moving vertically
        }
    }

    private void pitchToWaypoint(FlightControlData data, int[] distance){
        if(distance[0]>waypointAccuracy && distance[1]>waypointAccuracy){//If is not in the waypoint area
            data.setPitch(pitchVelocity);//TODO: set type to velocity (m/s)
        }
        else{
            data.setPitch(0);//Stop moving forward
        }
    }

    private void yawToBox(FlightControlData data){
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
        int [] distance = new int[3];
        for (int i = 0; i < 2; i++) {
            distance[i] = waypoint_pos[i] - current_pos[i];
        }
        return distance;
    }
    private int calculateHeight(int[] current_pos, int[] waypoint_pos) {
        return waypoint_pos[2] - current_pos[2];
    }

    public double getAngle() {
        return calculateAngle(current_angle, current_pos, waypoint_pos);
    }
}
