package com.dji.sdk.sample.internal.api;

import com.dji.sdk.sample.demo.flightcontroller.Logger;

public class MqttDataStore {
    private static MqttDataStore instance;
    private float[] position = {0,0,0};
    private float[] angle = {0,0,0,0};

    private Logger logger = Logger.getInstance();

    // Private constructor to prevent instantiation
    private MqttDataStore() {}

    // Get the singleton instance
    public static synchronized MqttDataStore getInstance() {
        if (instance == null) {
            instance = new MqttDataStore();
        }
        return instance;
    }

    // Setter method to store the position
    public void setPosition(float[] position) {
        //logger.log("setpos");
        this.position = position;
    }
    public void setAngle(float[] angle) {
        this.angle = angle;
    }
    // Getter method to retrieve the position
    public float[] getPosition() {
        return position;
    }
    public float[] getAngle() {
        return angle;
    }
}
