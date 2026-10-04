package com.example.photobooth;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Dùng 10.0.2.2 khi chạy trên máy ảo Android Emulator kết nối với Backend Flask trên máy tính
    private static final String BASE_URL = "http://10.0.2.2:5000/";
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
