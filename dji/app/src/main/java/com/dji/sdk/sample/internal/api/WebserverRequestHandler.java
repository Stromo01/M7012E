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
    private static final String BROKER = "tcp://192.168.10.194:1883";
    private static final String CLIENT_ID = "device-1234";
    private static final String TOPIC = "test";



    public static void startMQTTFlow(Context context) {
        ZeroKeyWaypoint zeroKeyWaypoint = new ZeroKeyWaypoint(context);
        try {

        MqttAndroidClient client = new MqttAndroidClient(context, BROKER, CLIENT_ID);
        MqttConnectOptions options = new MqttConnectOptions();

        // Callback handlers
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                ToastUtils.setResultToToast("Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                ToastUtils.setResultToToast("Received message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                ToastUtils.setResultToToast("Message delivered successfully!");
            }
        });

            // Connect and subscribe
            client.connect(options);
            ToastUtils.setResultToToast("Connected to broker.");

            client.subscribe(TOPIC, 0);
        }
        catch (MqttException e) {
            ToastUtils.setResultToToast("Error connecting to broker: " + e);
        }
        catch(Exception e) {
            zeroKeyWaypoint.logToFile("Error: " + e);
            ToastUtils.setResultToToast("Error: " + e);

        }

    }
}


