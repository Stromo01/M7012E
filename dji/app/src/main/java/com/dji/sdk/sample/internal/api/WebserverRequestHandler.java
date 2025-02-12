package com.dji.sdk.sample.internal.api;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.dji.sdk.sample.demo.flightcontroller.ZeroKeyWaypoint;
import com.dji.sdk.sample.internal.utils.ToastUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.*;
import java.nio.charset.StandardCharsets;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.security.MessageDigest;
import java.time.Instant;

public class WebserverRequestHandler {
    private static final String BROKER = "tcp://192.168.10.193:1883";
    private String CLIENT_ID;
    private static final String TOPIC = "test";
    private ZeroKeyWaypoint zeroKeyWaypoint;
    private MqttAndroidClient client;


    public void startMQTTFlow(Context context) {
        try {
            zeroKeyWaypoint = new ZeroKeyWaypoint(context);
        } catch (Exception e) {//Will always throw error if drone is not connected

        }

        try {
            CLIENT_ID=MqttClient.generateClientId();
            zeroKeyWaypoint.logToFile("Client id : " + CLIENT_ID);
            client = new MqttAndroidClient(context, BROKER, CLIENT_ID);
            connect();
            zeroKeyWaypoint.logToFile("End of MQTT flow");
        }
        catch(Exception e) {
            zeroKeyWaypoint.logToFile("Error: " + e);
            ToastUtils.setResultToToast("Error: " + e);

        }

    }
    private void sub(){
        try {
            client.subscribe(TOPIC, 0);
            zeroKeyWaypoint.logToFile("Subscribed to topic: " + TOPIC);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    zeroKeyWaypoint.logToFile("Connection lost: " + cause.getMessage());
                    ToastUtils.setResultToToast("Connection lost: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    zeroKeyWaypoint.logToFile("Received message: " + new String(message.getPayload()));
                    ToastUtils.setResultToToast("Received message: " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    zeroKeyWaypoint.logToFile("Message delivered successfully!");
                    ToastUtils.setResultToToast("Message delivered successfully!");
                }
            });
        }
        catch (MqttException e) {
            zeroKeyWaypoint.logToFile("Error subscribing to topic: " + e);
            ToastUtils.setResultToToast("Error subscribing to topic: " + e);
        }
    }

    private void connect(){
        try{
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    zeroKeyWaypoint.logToFile("Connected to broker.");
                    ToastUtils.setResultToToast("Connected to broker.");
                    sub();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    zeroKeyWaypoint.logToFile("Failed to connect to broker: " + exception.getMessage());
                    ToastUtils.setResultToToast("Failed to connect to broker: " + exception.getMessage());
                }
            });
        }
        catch (MqttException e) {
            zeroKeyWaypoint.logToFile("Error connecting to broker: " + e);
            ToastUtils.setResultToToast("Error connecting to broker: " + e);
        }

    }

}




