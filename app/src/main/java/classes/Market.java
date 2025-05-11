package classes;

import android.os.Build;

import java.time.LocalDate;

public class Market {
    private LocalDate date;
    private String location;

    public Market(LocalDate date, String location) {
        this.date = date;
        this.location = location;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Market{" +
                "date=" + date +
                ", location='" + location + '\'' +
                '}';
    }

}
