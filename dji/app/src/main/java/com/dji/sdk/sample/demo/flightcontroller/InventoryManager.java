



package com.dji.sdk.sample.demo.flightcontroller;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class InventoryManager {

    private static InventoryManager instance;



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
    private static void readData(String data){
        if (data.length()!= 10){
            System.out.println("Wrong kode");
        }
        try{
            int antal = Integer.parseInt(data.substring(0, 3));
            int produkt_ID = Integer.parseInt(data.substring(3, 6));


            System.out.println(" antal profukt i lådan: " + antal);
            System.out.println("vilken produkt finns i lådan: " + produkt_ID);

        }catch(NumberFormatException e){
            System.out.println(" fel vid av data ");

        }

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

    private InventoryManager() {
        // Initialize inventory
    }

    public void clearInventory() {
        // Clear inventory
    }

    private void readFile(){
        try {
            BufferedReader reader = new BufferedReader(new FileReader("inventoryData.txt"));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}