package com.google.firebase.samples.apps.mlkit;

import com.google.gson.annotations.SerializedName;

public class Response {

    @SerializedName("sucess")
    int sucess;

    @SerializedName("message")
    String message;

    public int getSucess() {
        return sucess;
    }

    public String getMessage() {
        return message;
    }

    public void setSucess(int sucess) {
        this.sucess = sucess;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Response() {
    }

    public Response(int sucess, String message) {
        this.sucess = sucess;
        this.message = message;
    }
}
