



package com.dji.sdk.sample.demo.flightcontroller;

public class InventoryManager {

    private static InventoryManager instance;

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
            System.out.println("fel kod");
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

    private void loadInventoryFile() {
        // Load inventory from file
    }
}
