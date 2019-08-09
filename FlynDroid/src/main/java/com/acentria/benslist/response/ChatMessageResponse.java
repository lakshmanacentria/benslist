package com.acentria.benslist.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatMessageResponse {

    @SerializedName("user_id")
    @Expose
    private String userId;
    @SerializedName("merchant_id")
    @Expose
    private String merchantId;
    @SerializedName("user_message")
    @Expose
    private String userMessage;
    @SerializedName("merchant_message")
    @Expose
    private String merchantMessage;
    @SerializedName("date")
    @Expose
    private String date;

    private String merchent_name;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getMerchantMessage() {
        return merchantMessage;
    }

    public void setMerchantMessage(String merchantMessage) {
        this.merchantMessage = merchantMessage;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }


    public void setMerchentName(String merchent_name) {
        this.merchent_name = merchent_name;
    }

    public String getMerchent_name() {
        return merchent_name;
    }
}
