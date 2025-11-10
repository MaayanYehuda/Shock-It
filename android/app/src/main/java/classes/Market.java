package classes;

import java.time.LocalDate;

public class Market {
    private LocalDate date;

    private String hours;
    private String location;
    private double latitude;
    private double longitude;

    public Market(LocalDate date, String location,String hours,double latitude, double longitude) {
        this.date = date;
        this.location = location;
        this.hours= hours;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LocalDate getDate() {
        return date;
    }



    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setHours(String hours){
        this.hours= hours;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getHours(){
        return this.hours;
    }


    @Override
    public String toString() {
        return "Market{" +
                "date=" + date +
                ", location='" + location + '\'' +
                '}';
    }

    public String getLocation() {
        return this.location;
    }
}
