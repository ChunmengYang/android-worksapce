package com.ycm.demo.db;

import android.graphics.Bitmap;

public class Book {
    private int id;
    private String name;
    private String desc;
    private Bitmap cover;

    public Book(int id, String name, String desc, Bitmap cover) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.cover = cover;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Bitmap getCover() {
        return cover;
    }

    public void setCover(Bitmap cover) {
        this.cover = cover;
    }
}
