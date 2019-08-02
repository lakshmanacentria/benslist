package com.acentria.benslist.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DonateCharityResponse {


    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("account_id")
    @Expose
    private String accountId;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("name_of_organization")
    @Expose
    private String nameOfOrganization;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("image")
    @Expose
    private String image;
    @SerializedName("c_type")
    @Expose
    private String cType;
    @SerializedName("amount")
    @Expose
    private String amount;
    @SerializedName("remaining_amount")
    @Expose
    private String remainingAmount;
    @SerializedName("email")
    @Expose
    private String email;
    @SerializedName("tel")
    @Expose
    private String tel;
    @SerializedName("product")
    @Expose
    private String product;
    @SerializedName("quantity")
    @Expose
    private String quantity;
    @SerializedName("quantity_remain")
    @Expose
    private String quantityRemain;
    @SerializedName("country")
    @Expose
    private String country;
    @SerializedName("state")
    @Expose
    private String state;
    @SerializedName("city")
    @Expose
    private String city;
    @SerializedName("address")
    @Expose
    private String address;
    @SerializedName("payment_method")
    @Expose
    private String paymentMethod;
    @SerializedName("paypal_email")
    @Expose
    private String paypalEmail;
    @SerializedName("bank_name")
    @Expose
    private String bankName;
    @SerializedName("account_no")
    @Expose
    private String accountNo;
    @SerializedName("ifsc_code")
    @Expose
    private String ifscCode;
    @SerializedName("additional_info")
    @Expose
    private String additionalInfo;
    @SerializedName("posted_date")
    @Expose
    private String postedDate;
    @SerializedName("status")
    @Expose
    private String status;
    @SerializedName("ref_number")
    @Expose
    private String refNumber;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNameOfOrganization() {
        return nameOfOrganization;
    }

    public void setNameOfOrganization(String nameOfOrganization) {
        this.nameOfOrganization = nameOfOrganization;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(String postedDate) {
        this.postedDate = postedDate;
    }

    public String getTel() {
        return tel;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }


    public String getRefNumber() {
        return refNumber;
    }

    public void setRefNumber(String refNumber) {
        this.refNumber = refNumber;
    }


    public String getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(String remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getQuantityRemain() {
        return quantityRemain;
    }

    public void setQuantityRemain(String quantityRemain) {
        this.quantityRemain = quantityRemain;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
