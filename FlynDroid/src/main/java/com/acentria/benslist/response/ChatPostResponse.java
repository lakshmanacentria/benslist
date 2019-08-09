package com.acentria.benslist.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatPostResponse {

    @SerializedName("user_id")
    @Expose
    private String userId;
    @SerializedName("merchant_id")
    @Expose
    private String merchantId;
    @SerializedName("post_id")
    @Expose
    private String postId;
    @SerializedName("Main_photo")
    @Expose
    private String mainPhoto;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("job_position")
    @Expose
    private String jobPosition;
    private String username="";



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

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getMainPhoto() {
        return mainPhoto;
    }

    public void setMainPhoto(String mainPhoto) {
        this.mainPhoto = mainPhoto;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getJobPosition() {
        return jobPosition;
    }

    public void setJobPosition(String jobPosition) {
        this.jobPosition = jobPosition;
    }

    public void setUserName(String usernaeme) {
        this.username=usernaeme;
    }


    public String getUsername(){
        return username;
    }


}
