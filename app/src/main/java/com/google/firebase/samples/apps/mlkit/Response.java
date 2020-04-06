package com.google.firebase.samples.apps.mlkit;

import com.google.gson.annotations.SerializedName;

public class Response {

    @SerializedName("focused")
    boolean focused;

    public Response(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
