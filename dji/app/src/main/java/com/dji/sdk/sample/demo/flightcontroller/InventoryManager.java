



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

    public String getInventoryList() {
        return "Inventory list";
    }

    private String getInventoryItemById(String id) {
        return "Inventory item";
    }

    public void addInventoryItem(String item) {
        // Add item to inventory
    }
    private String[] readData(String data){

        String quantity = data.substring(0, 2);
        String ID = data.substring(3, 5);

        String[] outData = {quantity, ID};
        return outData;

    }

    public void removeInventoryItem(String id) {
        // Remove item from inventory
    }

    public void updateInventoryItem(String id, String item) {
        // Update item in inventory
    }

    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }
    private void addItemToFile(String[] productInfo, int quantity){
        
    }


    public void clearInventory() {
        // Clear inventory
    }

}
    public static void addItemToFile(String[] dataArray, String quantity){
    try {
        String newConstructedline = dataArray[0] + dataArray[1] + dataArray[2] + quantity;
        // creates an empty list
        //String lines[] = {};
        //ArrayList<String> lines = new ArrayList<>();
        // creates a Path instance based on filePath.
        Path path = Paths.get(filePath);

        //if (Files.exists(path)){
            // Read all rows and store in lines
            //lines = Files.readAllLines(path);
        //}
        // convert the array to a string with " : " as separator

        String newLine = String.join(" : ", dataArray);
        newLine = String.join(" : ", quantity);

        // Add the new row to the list
        //lines.add(newLine);
        // (CREATE) creates file if it does not exist and (appends) the new line at the bottom of the file
        Files.write(path, Collections.singletonList(newLine), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.println("data: " + newLine);

    }catch (IOException e){
        System.err.println("File handling error: " + e.getMessage());
    }

    }

