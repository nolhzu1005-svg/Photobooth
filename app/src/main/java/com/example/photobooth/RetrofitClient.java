package com.example.photobooth;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class RetrofitClient {
    // Nếu chạy trên máy ảo Android Emulator -> dùng "http://10.0.2.2:5000/"
    // Nếu chạy trên điện thoại thật -> dùng IP Wi-Fi máy tính (vd: "http://192.168.100.136:5000/")
    private static final String BASE_URL = "http://192.168.100.136:5000/";
    private static Retrofit retrofit = null;
    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}
