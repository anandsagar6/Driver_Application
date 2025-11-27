package com.example.driver.api;

import com.example.driver.model.UploadResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @Multipart
    @POST("{phone}/upload")
    Call<UploadResponse> uploadFile(
            @Path("phone") String phone,
            @Part MultipartBody.Part file,
            @Part("type") RequestBody type
    );
}
