package com.johendry.wisatator;

import java.io.Serializable;

public class Penginapan implements Serializable {
    private int id;
    private String nama;
    private String deskripsi;
    private String alamat;
    private Double rating;
    private int hargaPerMalam;
    private String imageUrl; // untuk url gambar dari Firestore

    private Double latitude;   // posisi penginapan
    private Double longitude;  // posisi penginapan

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

    public String getAlamat() {
        return alamat;
    }

    public void setAlamat(String alamat) {
        this.alamat = alamat;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public int getHargaPerMalam() {
        return hargaPerMalam;
    }

    public void setHargaPerMalam(int hargaPerMalam) {
        this.hargaPerMalam = hargaPerMalam;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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

    // Helper untuk adapter (fallback kalau imageUrl kosong)
    public Object getGambarRes() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl; // Glide bisa load URL
        }
        return R.drawable.ic_launcher_background; // fallback
    }
}
