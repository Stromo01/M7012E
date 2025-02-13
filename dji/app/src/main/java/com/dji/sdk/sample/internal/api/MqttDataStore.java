package com.dji.sdk.sample.internal.api;

public class MqttDataStore {
    private static MqttDataStore instance;
    private String position;
    private String angle;

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
    public void setPosition(String position) {
        this.position = position;
    }

    // Getter method to retrieve the position
    public String getPosition() {
        return position;
    }
    public String getAngle() {
        return angle;
    }
}
