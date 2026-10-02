package com.example.photobooth;

public class Frame {
    private String id;
    private String name;
    private int colorHex;

    public Frame(String id, String name, int colorHex) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getColorHex() { return colorHex; }
}
