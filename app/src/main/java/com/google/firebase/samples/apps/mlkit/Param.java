package com.google.firebase.samples.apps.mlkit;

import com.google.gson.annotations.SerializedName;

public class Param {
    @SerializedName("url")
    String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Param() {
    }

    public Param(String url) {
        this.url = url;
    }
}
