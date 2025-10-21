package com.johendry.wisatator;

import java.io.Serializable;

public class Kuliner implements Serializable {
    private int id;
    private String nama;
    private String deskripsi;
    private String lokasi;     // alamat teks
    private String catatan;
    private int gambarRes;     // fallback resource ID
    private String imageUrl;   // kalau gambar dari Firestore URL
    private Double rating;     // ⭐ rating kuliner
    private Double latitude;   // koordinat Maps
    private Double longitude;  // koordinat Maps

    public Kuliner() {
    }

    // --- Getter & Setter ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public String getDeskripsi() {
        return deskripsi;
    }

    public void setDeskripsi(String deskripsi) {
        this.deskripsi = deskripsi;
    }

    public String getLokasi() {
        return lokasi;
    }

    public void setLokasi(String lokasi) {
        this.lokasi = lokasi;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }

    public int getGambarRes() {
        return gambarRes;
    }

    public void setGambarRes(int gambarRes) {
        this.gambarRes = gambarRes;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
