package com.acentria.benslist.response;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Country {

    private int countryID;
    @SerializedName("Country_name")
    @Expose
    private String countryName;

    public Country(/*int countryID,*/ String countryName) {
//        this.countryID = countryID;
        this.countryName = countryName;
    }


    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }
    @Override
    public String toString() {
        return countryName;
    }

//    @Override
//    public int compareTo(Country another) {
//        return this.countryID-another.countryID;
//    }
}
