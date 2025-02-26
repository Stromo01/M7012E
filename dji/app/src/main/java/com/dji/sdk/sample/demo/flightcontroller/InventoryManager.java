



package com.dji.sdk.sample.demo.flightcontroller;

import android.text.style.LineBackgroundSpan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.ArrayList;


public class InventoryManager {

    private static InventoryManager instance;

    private Logger logger = new Logger();

    private InventoryManager() {
        // Initialize inventory
    }

    public void submitQrResult(String qrResult) {
        String[] data = readData(qrResult);
        String quantity = data[0];
        String[] productInfo = serchProductInfoWithID(data[1]);
        if (productInfo.length == 3) {
            logger.log("Product found: " + productInfo[0] + ", " + productInfo[1] + ", " + productInfo[2]);
        } else {
            logger.log("Product not found");
        }
        addItemToFile(productInfo, quantity);
    }

    private void addItemToFile(String[] productInfo, String quantity) {
        try {
            File file = new File("app/src/main/java/com/dji/sdk/sample/demo/flightcontroller/Inventory.txt");
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            String newLine = String.join(" : ", productInfo) + " : " + quantity;
            out.println(newLine);
            out.close();
            System.out.println("data: " + newLine);

        } catch (IOException e) {
            System.err.println("File handling error: " + e.getMessage());
        }
    }

    private JSONArray loadInventoryFile() throws JSONException {
        JSONArray inventoryArray = null;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("app/src/main/java/com/dji/sdk/sample/demo/flightcontroller/ProduktInfo.json"));
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            reader.close();
            inventoryArray = new JSONArray(jsonContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inventoryArray;
    }

    private String[] serchProductInfoWithID(String id) {
        JSONArray inventoryArray = null;
        try {
            inventoryArray = loadInventoryFile();
        } catch (JSONException e) {
            e.printStackTrace();
            return new String[]{"Error loading inventory"};
        }
        if (inventoryArray != null) {
            for (int i = 0; i < inventoryArray.length(); i++) {
                JSONObject item = null;
                try {
                    item = inventoryArray.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                    continue;
                }
                try {
                    if (item.getString("id").equals(id)) {
                        return new String[]{item.getString("name"), item.getString("size"), item.getString("color")};
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        return new String[]{"Product not found"};
    }

    private String[] readData(String data){

        String quantity = data.substring(0, 2);
        String ID = data.substring(3, 5);

        String[] outData = {quantity, ID};
        return outData;

    }


    public InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }



}





