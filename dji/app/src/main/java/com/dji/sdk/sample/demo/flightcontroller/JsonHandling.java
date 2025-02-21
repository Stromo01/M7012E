package com.dji.sdk.sample.demo.flightcontroller;

import com.dji.sdk.sample.demo.flightcontroller.Logger;

import com.dji.sdk.sample.internal.api.MqttDataStore;
import com.dji.sdk.sample.internal.api.WebserverRequestHandler;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import android.content.Context;
import android.util.Log;



public class JsonHandling {

    private Logger logger = Logger.getInstance();
    private WebserverRequestHandler server;
    private Context context;
    public void setWaypointZeroKey(Context context) {
        this.context = context;
        server = new WebserverRequestHandler();
        server.startMQTTFlow(context);
        System.out.println(new File(".").getAbsolutePath());
        Path path = Paths.get("app\\src\\main\\java\\com\\dji\\sdk\\sample\\demo\\flightcontroller");
        float[] cur_pos = MqttDataStore.getInstance().getPosition(); // Your float position
        for (int i = 0; i < cur_pos.length; i++) {
            logger.log("Position " + i + ": " + cur_pos[i]);
        }


        try {
            JSONArray jsonArray;
            logger.log("reached");
            if (Files.exists(path)) {
                String content = new String(Files.readAllBytes(path));
                if (!content.trim().isEmpty()) {
                    logger.log("file is empty");
                    jsonArray = new JSONArray(content);
                } else {
                    jsonArray = new JSONArray();
                }
            } else {
                logger.log("reached3");
                jsonArray = new JSONArray(); // Create new if file doesn't exist
            }

            // Convert float array to JSONArray
            JSONArray positionArray = new JSONArray();
            for (float pos : cur_pos) {
                logger.log("reached4");
                positionArray.put(pos);
            }
            logger.log("reached5");
            jsonArray.put(positionArray); // Append new position array
            logger.log("what to write"+ jsonArray.toString(4).getBytes());
            // Write back to file

            Files.write(path, jsonArray.toString(4).getBytes()); // Pretty-print with indentation

        } catch (Exception e) {
            logger.log("reached catch"+e.getMessage());
            e.printStackTrace();
        }
    }
}
