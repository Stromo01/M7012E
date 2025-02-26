



package com.dji.sdk.sample.demo.flightcontroller;

import android.text.style.LineBackgroundSpan;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.ArrayList;


public class InventoryManager {

    private static final Log log = LogFactory.getLog(InventoryManager.class);
    private static InventoryManager instance;

    private Logger logger;

    public InventoryManager() {
        // Initialize inventory
        logger = new Logger();
    }

    public void submitQrResult(String qrResult) {
        String[] data = readData(qrResult);
        logger.log("row 38");
        String quantity = data[0];
        logger.log("row 40");
        String[] productInfo = serchProductInfoWithID(data[1]);
        logger.log("row 41");
        //if (productInfo.length == 3) {
        //    logger.log("Product found: " + productInfo[0] + ", " + productInfo[1] + ", " + productInfo[2]);
        //} else {
        //    logger.log("Product not found");
        //}
        addItemToFile(productInfo, quantity);
    }

    private void addItemToFile(String[] productInfo, String quantity) {
        String newLine = String.join(" : ", productInfo) + " : " + quantity;
        logger.log(newLine);
        try {
            File file = new File("app/src/main/java/com/dji/sdk/sample/demo/flightcontroller/Inventory.txt");
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            newLine = String.join(" : ", productInfo) + " : " + quantity;
            out.println(newLine);
            out.close();
            logger.log(newLine);
            System.out.println("data: " + newLine);

        } catch (IOException e) {
            logger.log(newLine + "error");
        }
    }

    private JSONArray loadInventoryFile() throws JSONException {
        logger.log("row 75");
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
        logger.log("row 93");
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
        logger.log("row 118");

        String quantity = data.substring(0, 3);
        logger.log("row 127");
        String ID = data.substring(4, 7);
        logger.log("row 129");
        String[] outData = {quantity, ID};
        logger.log(outData[0] + outData[1]);
        return outData;

    }


    public InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }



}





