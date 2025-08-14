package classes;

import java.util.HashMap;
import java.util.TreeSet;

public class Farmer extends User{
    private HashMap<Item, Double> products;
    private TreeSet<FarmerMarket> markets;
    private double latitude;
    private double longitude;
    private int notificationRadius;
    public Farmer(String name, String email, String password, String phone, String address, double latitude, double longitude, int notificationRadius) {
        super(name, email, password, phone, address);
        this.products = new HashMap<>();
        this.markets = new TreeSet<>();
        this.latitude = latitude;
        this.longitude = longitude;
        this.notificationRadius = notificationRadius;
    }
    public Farmer(String name, String email) {
        super(name, email, null, null, null);
    }



    public Farmer(String name, String email, String password, String phone, String address) {
        super(name, email, password, phone, address);
        this.products = new HashMap<>();
        this.markets = new TreeSet<>();
    }



    public void addFarmerMarket(FarmerMarket farmerMarket) {
        this.markets.add(farmerMarket);
    }


    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getNotificationRadius() {
        return notificationRadius;
    }

    public void setNotificationRadius(int notificationRadius) {
        this.notificationRadius = notificationRadius;
    }

    public TreeSet<FarmerMarket> getMarkets() {
        return this.markets;
    }
    public void addProduct(Item item, double price){
        products.put(item, price);
    }
    public Double getPrice(Item item){
        return products.get(item);
    }
    public HashMap<Item, Double> getProducts(){
        return this.products;
    }
}
