package com.dji.sdk.sample.demo.flightcontroller;


import android.content.Context;

import java.io.IOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.*;
import java.util.Arrays;

public class InventoryManager {

    private static final Log log = LogFactory.getLog(InventoryManager.class);
    private static InventoryManager instance;

    private Logger logger;

    private Context context;

    public InventoryManager(Context context) {
        // Initialize inventory
        logger = Logger.getInstance();
        this.context = context;
    }

    public void submitQrResult(String qrResult) {
        //loadJSONFromAsset();
        logger.log("submitQrResult called with: " + qrResult);
        String[] data = readData(qrResult);
        logger.log("Parsed data: " + Arrays.toString(data));
        String quantity = data[0];
        logger.log("Quantity: " + quantity);
        String[] productInfo = serchProductInfoWithID(data[1]);
        logger.log("Product info: " + Arrays.toString(productInfo));
        if (productInfo.length == 3) {
            logger.log("Product found: " + productInfo[0] + ", " + productInfo[1] + ", " + productInfo[2]);
        } else {
            logger.log("Product not found");
        }
        addItemToFile(productInfo, quantity);
    }

    private void addItemToFile(String[] productInfo, String quantity) {
        String newLine = "{\"name\": \"" + productInfo[0] + "\", \"size\": \"" + productInfo[1] + "\", \"color\": \"" + productInfo[2] + "\", \"quantity\": \"" + quantity + "\"}";
        logger.log("Adding item to file: " + newLine);
        try {
            File file = new File(context.getFilesDir(), "InventoryFile.json");
            if (!file.exists()) {
                file.createNewFile();
                logger.log("Created new file: " + file.getPath());
            }

            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw);

            out.println(newLine);
            out.close();
            logger.log("Successfully added item to file: " + newLine);
        } catch (IOException e) {
            logger.log("Error adding item to file: " + e);
        }
    }

    public String loadJSONFromAsset(Context applicationContext) {
        logger.log("Loading JSON from asset");
        String json = null;
        try {
            InputStream is = applicationContext.getApplicationContext().getAssets().open("ProduktInfo.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            logger.log("Error loading JSON from asset: " + ex.getMessage());

            return null;
        }
        logger.log("Loaded JSON from asset: " + json);
        return json;
    }
    private JSONArray loadInventoryFile() throws JSONException {
        logger.log("Loading inventory file");
        JSONArray inventoryArray = null;
        String json = loadJSONFromAsset(context);
        inventoryArray = new JSONArray(json);
        logger.log("Successfully loaded inventory file");
        logger.log("Inventory: " + inventoryArray.toString());
        return inventoryArray;
    }

    private String[] serchProductInfoWithID(String id) {
        logger.log("Searching product info with ID: " + id);
        JSONArray inventoryArray;
        try {
            inventoryArray = loadInventoryFile();
            if (inventoryArray == null || inventoryArray.length() == 0) {
                logger.log("Inventory is empty or null");
                return new String[]{"Error loading inventory"};
            }
            for (int i = 0; i < inventoryArray.length(); i++) {
                JSONObject item = inventoryArray.getJSONObject(i);
                if (item.getString("id").equals(id)) {
                    logger.log("Product found: " + item.getString("name"));
                    return new String[]{
                            item.getString("name"),
                            item.getString("size"),
                            item.getString("color")
                    };
                }
            }
        } catch (JSONException e) {
            logger.log("Error processing inventory JSON: " + e.getMessage());
            return new String[]{"Error processing inventory JSON"};
        }
        logger.log("Product not found with ID: " + id);
        return new String[]{"Product not found"};
    }

    private String[] readData(String data) {
        logger.log("Reading data: " + data);
        String quantity = data.substring(0, 3);
        logger.log("Extracted quantity: " + quantity);
        String ID = data.substring(3, 6);
        logger.log("Extracted ID: " + ID);
        String[] outData = {quantity, ID};
        logger.log("Parsed data: " + Arrays.toString(outData));
        return outData;
    }


}