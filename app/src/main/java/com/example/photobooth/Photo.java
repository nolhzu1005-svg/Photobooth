package com.example.photobooth;

public class Photo {
    private String base64Data;
    private String processedBase64;

    public Photo(String base64Data) {
        this.base64Data = base64Data;
        this.processedBase64 = base64Data;
    }

    public String getBase64Data() { return base64Data; }
    public void setBase64Data(String base64Data) { this.base64Data = base64Data; }

    public String getProcessedBase64() { return processedBase64; }
    public void setProcessedBase64(String processedBase64) { this.processedBase64 = processedBase64; }
}
