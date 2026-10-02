package com.example.photobooth;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
public interface ApiService {
    // 1. Áp dụng bộ lọc OpenCV
    @POST("api/process-filter")
    Call<FilterResponse> processFilter(@Body FilterRequest request);
    // 2. Ghép chuỗi ảnh Photostrip Pillow
    @POST("api/create-strip")
    Call<StripResponse> createStrip(@Body StripRequest request);
    // 3. Tạo ảnh GIF chuyển động
    @POST("api/generate-gif")
    Call<GifResponse> generateGif(@Body GifRequest request);
    // --- REQUEST & RESPONSE MODELS ---
    class FilterRequest {
        @SerializedName("image") public String image;
        @SerializedName("filter_name") public String filterName;
        @SerializedName("beauty_level") public float beautyLevel;
        @SerializedName("brightness") public int brightness;
        @SerializedName("contrast") public int contrast;
        @SerializedName("saturation") public int saturation;
        public FilterRequest(String image, String filterName, float beautyLevel, int brightness, int contrast, int saturation) {
            this.image = image;
            this.filterName = filterName;
            this.beautyLevel = beautyLevel;
            this.brightness = brightness;
            this.contrast = contrast;
            this.saturation = saturation;
        }
    }
    class FilterResponse {
        @SerializedName("status") public String status;
        @SerializedName("image_processed") public String imageProcessed;
    }
    class StripRequest {
        @SerializedName("images") public List<String> images;
        @SerializedName("layout") public String layout;
        @SerializedName("theme") public String theme;
        @SerializedName("custom_text") public String customText;
        @SerializedName("date_text") public String dateText;
        public StripRequest(List<String> images, String layout, String theme, String customText, String dateText) {
            this.images = images;
            this.layout = layout;
            this.theme = theme;
            this.customText = customText;
            this.dateText = dateText;
        }
    }
    class StripResponse {
        @SerializedName("status") public String status;
        @SerializedName("strip_url") public String stripUrl;
        @SerializedName("qr_url") public String qrUrl;
        @SerializedName("b64_strip") public String b64Strip;
    }
    class GifRequest {
        @SerializedName("images") public List<String> images;
        public GifRequest(List<String> images) { this.images = images; }
    }
    class GifResponse {
        @SerializedName("status") public String status;
        @SerializedName("gif_url") public String gifUrl;
        @SerializedName("b64_gif") public String b64Gif;
    }
}
