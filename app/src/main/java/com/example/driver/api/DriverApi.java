package com.example.driver.api;

import com.example.driver.model.UploadResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface DriverApi {



    @Multipart
    @POST("/api/driver/upload")
    Call<UploadResponse> uploadDocument(
            @Part("phone") RequestBody phone,
            @Part("type") RequestBody type,
            @Part MultipartBody.Part file
    );


    @GET("/getDocument")
    Call<UploadResponse> getDocument(
            @Query("phone") String phone,
            @Query("type") String type
    );
}
