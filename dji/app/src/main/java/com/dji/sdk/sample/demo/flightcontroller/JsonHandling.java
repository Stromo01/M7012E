package com.dji.sdk.sample.demo.flightcontroller;

import com.dji.sdk.sample.internal.api.WebserverRequestHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;

import android.content.Context;

import java.io.*;
import java.util.List;




public class JsonHandling {

    private Logger logger = Logger.getInstance();
    private File waypointsFile;
    private WebserverRequestHandler server;
    private Context context;


    public List<Waypoints> setWaypointZeroKey(Context applicationContext) {
        logger = Logger.getInstance();


        // Define directory and file paths
        String json = null;
        try {
            // Ensure directory exists
            InputStream is = applicationContext.getApplicationContext().getAssets().open("waypoints.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");


        } catch (Exception e) {
            logger.log("Error handling waypoints file: " + e.getMessage());
            e.printStackTrace();
        }

        Gson gson = new Gson();
        Type listType = new TypeToken<List<Waypoints>>() {}.getType();

        List<Waypoints> waypointsList = gson.fromJson(json, listType);

        // Log the entire list

        // Print each waypoint individually
        return waypointsList;
    }



}

