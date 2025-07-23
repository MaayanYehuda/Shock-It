package classes;

import java.util.HashMap;
import java.util.TreeSet;

public class Farmer extends User{
    private HashMap<Item, Double> products;
    private TreeSet<FarmerMarket> markets;

    public Farmer(String name, String email, String password, String phone, String address) {
        super(name, email, password, phone, address);
        this.products = new HashMap<>();
        this.markets = new TreeSet<>();
    }
    public void addFarmerMarket(FarmerMarket farmerMarket) {
        this.markets.add(farmerMarket);
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
