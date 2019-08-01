package com.acentria.benslist.response;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class City  {

    private Country country;
    private State state;
    @SerializedName("City_name")
    @Expose
    private String cityName;

    public City(/*int cityID*//*,*/ Country country, State state, String cityName) {
//        this.cityID = cityID;
        this.country = country;
        this.state = state;
        this.cityName = cityName;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    @Override
    public String toString() {
        return cityName;
    }
}
