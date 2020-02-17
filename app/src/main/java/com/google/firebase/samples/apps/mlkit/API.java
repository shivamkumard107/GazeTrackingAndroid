package com.google.firebase.samples.apps.mlkit;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface API {

    @Multipart
    @POST("image")
    Call<Response> upload(
            @Part MultipartBody.Part video
    );

    @POST("post")
    Call<Response> send_url(
            @Query("name") String url
    );
}
