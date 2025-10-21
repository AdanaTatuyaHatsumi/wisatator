package com.johendry.wisatator;

import java.io.Serializable;

public class Wisata implements Serializable {
    private String id;
    private String nama;
    private String deskripsi;
    private String lokasi;
    private double latitude;
    private double longitude;
    private int gambarRes; // fallback drawable resource id
    private String imageUrl; // url (Firestore will store this)

    public Wisata() { }

    public Wisata(String nama, String deskripsi, String lokasi, String imageUrl) {
        this.nama = nama;
        this.deskripsi = deskripsi;
        this.lokasi = lokasi;
        this.imageUrl = imageUrl;
        this.gambarRes = 0;
    }

    // getters & setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNama() { return nama; }
    public void setNama(String nama) { this.nama = nama; }

    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    public String getLokasi() { return lokasi; }
    public void setLokasi(String lokasi) { this.lokasi = lokasi; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getGambarRes() { return gambarRes; }
    public void setGambarRes(int gambarRes) { this.gambarRes = gambarRes; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public String toString() {
        return "Wisata{" + "nama='" + nama + '\'' + ", lokasi='" + lokasi + '\'' + '}';
    }
}
