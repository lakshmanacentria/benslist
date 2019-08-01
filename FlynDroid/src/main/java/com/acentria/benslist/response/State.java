package com.acentria.benslist.response;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class State {

    private Country country;
    @SerializedName("Region_name")
    @Expose
    private String stateName;
    private String countryName;


    public State(Country country, String stateName) {

        this.country = country;
        this.stateName = stateName;
    }


    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }


//    @Override
//    public String toString() {
//        return "State{" +
//                "country=" + country +
//                ", stateName='" + stateName + '\'' +
//                ", countryName='" + countryName + '\'' +
//                '}';
//    }

    @Override
    public String toString() {
        return stateName;
    }
}
