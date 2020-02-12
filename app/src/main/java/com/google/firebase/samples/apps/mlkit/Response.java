package com.google.firebase.samples.apps.mlkit;

import com.google.gson.annotations.SerializedName;

public class Response {

    @SerializedName("sucess")
    boolean sucess;

    @SerializedName("message")
    String message;

    public boolean isSucess() {
        return sucess;
    }

    public String getMessage() {
        return message;
    }
}
