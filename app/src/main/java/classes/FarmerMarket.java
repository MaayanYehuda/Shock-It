package classes;

import android.os.Build;

public class FarmerMarket implements  Comparable<FarmerMarket>{
    private Market market;
    private boolean participated;

    public FarmerMarket(Market market) {
        this.market = market;
    }

    public Market getMarket() {
        return market;
    }

    public void setParticipated(boolean participated) {
        this.participated = participated;
    }

    public boolean isParticipated() {
        return participated;
    }

    @Override
    public int compareTo(FarmerMarket other) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return market.getDate().compareTo(other.market.getDate());
        }
        return -1;
    }
}
