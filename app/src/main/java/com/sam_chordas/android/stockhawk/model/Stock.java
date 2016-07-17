package com.sam_chordas.android.stockhawk.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by adam on 16-07-13.
 */
public class Stock implements Parcelable {

    public static final String TAG = Stock.class.getSimpleName();

    private String name;
    private String symbol;
    private String bid;
    private String percentChange;
    private String change;
    private String dayLow;
    private String dayHigh;
    private String yearLow;
    private String yearHigh;
    private String volume;
    private String historicalData;

    public Stock() {}

    public Stock(Parcel in) {
        name = in.readString();
        symbol = in.readString();
        bid = in.readString();
        percentChange = in.readString();
        change = in.readString();
        dayLow = in.readString();
        dayHigh = in.readString();
        yearLow = in.readString();
        yearHigh = in.readString();
        volume = in.readString();
        historicalData = in.readString();
    }

    public static final Creator<Stock> CREATOR = new Creator<Stock>() {
        @Override
        public Stock createFromParcel(Parcel in) {
            return new Stock(in);
        }

        @Override
        public Stock[] newArray(int size) {
            return new Stock[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(symbol);
        dest.writeString(bid);
        dest.writeString(percentChange);
        dest.writeString(change);
        dest.writeString(dayLow);
        dest.writeString(dayHigh);
        dest.writeString(yearLow);
        dest.writeString(yearHigh);
        dest.writeString(volume);
        dest.writeString(historicalData);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public String getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(String percentChange) {
        this.percentChange = percentChange;
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getDayLow() {
        return dayLow;
    }

    public void setDayLow(String dayLow) {
        this.dayLow = dayLow;
    }

    public String getDayHigh() {
        return dayHigh;
    }

    public void setDayHigh(String dayHigh) {
        this.dayHigh = dayHigh;
    }

    public String getYearLow() {
        return yearLow;
    }

    public void setYearLow(String yearLow) {
        this.yearLow = yearLow;
    }

    public String getYearHigh() {
        return yearHigh;
    }

    public void setYearHigh(String yearHigh) {
        this.yearHigh = yearHigh;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getHistoricalData() {
        return historicalData;
    }

    public void setHistoricalData(String historicalData) {
        this.historicalData = historicalData;
    }
}