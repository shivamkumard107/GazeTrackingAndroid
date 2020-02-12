package com.google.firebase.samples.apps.mlkit;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface API {

    @Multipart
    @POST("upload")
    Call<Response> upload(
            @Part MultipartBody.Part video
    );
}