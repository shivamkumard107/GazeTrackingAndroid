package com.google.firebase.samples.apps.mlkit;

import com.google.gson.annotations.SerializedName;

public class JsonResponse {

    @SerializedName("score")
    float Message;

    public JsonResponse(float message) {
        Message = message;
    }

    public float getMessage() {
        return Message;
    }

    public void setMessage(float message) {
        Message = message;
    }
}
