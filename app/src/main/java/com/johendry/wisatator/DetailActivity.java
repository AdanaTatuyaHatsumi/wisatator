package com.johendry.wisatator;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetailActivity extends AppCompatActivity {

    private static final String TAG = "DetailActivity";

    private ImageView ivGambar;
    private TextView tvNama, tvLokasi, tvDeskripsi, tvExtra;
    private RatingBar ratingBar;
    private TextView tvRatingValue, tvPrice;
    private MaterialButton btnOpenMaps;

    private Object currentModel;

    // Firestore & Gallery
    private FirebaseFirestore firestore;
    private RecyclerView recyclerGallery;
    private GalleryAdapter galleryAdapter;

    public static final String EXTRA_COLLECTION = "extra_collection";
    public static final String EXTRA_DOCID = "extra_docid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        Log.d(TAG, "onCreate");

        ivGambar = findViewById(R.id.ivGambar);
        tvNama = findViewById(R.id.tvNama);
        tvLokasi = findViewById(R.id.tvLokasi);
        tvDeskripsi = findViewById(R.id.tvDeskripsi);
        tvExtra = findViewById(R.id.tvExtra);
        ratingBar = findViewById(R.id.ratingBarDetail);
        tvRatingValue = findViewById(R.id.tvRatingValue);
        tvPrice = findViewById(R.id.tvPrice);
        btnOpenMaps = findViewById(R.id.btnOpenMaps);

        firestore = FirebaseFirestore.getInstance();

        recyclerGallery = findViewById(R.id.recyclerGallery);
        if (recyclerGallery != null) {
            LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            recyclerGallery.setLayoutManager(lm);
            recyclerGallery.setHasFixedSize(true);
            recyclerGallery.setNestedScrollingEnabled(false);
            galleryAdapter = new GalleryAdapter(this, new ArrayList<>());
            recyclerGallery.setAdapter(galleryAdapter);
            recyclerGallery.setVisibility(View.GONE);
            Log.d(TAG, "RecyclerView and adapter initialized");
        } else {
            Log.w(TAG, "recyclerGallery not found in layout");
        }

        // Debug extras
        Intent i = getIntent();
        if (i != null && i.getExtras() != null) {
            for (String k : i.getExtras().keySet()) {
                Log.d(TAG, "Intent extra [" + k + "] = " + String.valueOf(i.getExtras().get(k)));
            }
        }

        Object wisata = getIntent().getSerializableExtra("wisata");
        Object kuliner = getIntent().getSerializableExtra("kuliner");
        Object penginapan = getIntent().getSerializableExtra("penginapan");
        String extraImageUrl = getIntent().getStringExtra("imageUrl");

        if (extraImageUrl != null && !extraImageUrl.trim().isEmpty()) loadImage(extraImageUrl);

        if (wisata != null) { currentModel = wisata; bindWisata(wisata, extraImageUrl); }
        else if (kuliner != null) { currentModel = kuliner; bindKuliner(kuliner, extraImageUrl); }
        else if (penginapan != null) { currentModel = penginapan; bindPenginapan(penginapan, extraImageUrl); }
        else {
            String name = getIntent().getStringExtra("name");
            String lokasi = getIntent().getStringExtra("lokasi");
            String desc = getIntent().getStringExtra("description");
            String imageUrl = extraImageUrl != null ? extraImageUrl : getIntent().getStringExtra("imageUrl");
            tvNama.setText(name != null ? name : getString(R.string.placeholder_name));
            tvLokasi.setText(lokasi != null ? lokasi : getString(R.string.placeholder_location));
            tvDeskripsi.setText(desc != null ? desc : "");
            if (imageUrl != null && !imageUrl.trim().isEmpty()) loadImage(imageUrl);
            setupLocationClick(lokasi, null, null);
            ratingBar.setVisibility(View.GONE);
            tvRatingValue.setVisibility(View.GONE);
            tvPrice.setVisibility(View.GONE);
            tvExtra.setText("");
        }

        TabLayout tabLayout = findViewById(R.id.tabLayoutDetail);
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case 0: showInfoTab(); break;
                        case 1: showGalleryTab(); break;
                        case 2: showReviewsTab(); break;
                    }
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) {}
            });
        }

        showInfoTab();

        // PREFETCH gallery: model -> intent -> firestore
        ArrayList<String> fromModel = getImageListFromModel(currentModel);
        if (!fromModel.isEmpty()) {
            Log.d(TAG, "Prefetch: images found in model: size=" + fromModel.size());
            // normalisasi drive links
            ArrayList<String> norm = new ArrayList<>();
            for (String u : fromModel) norm.add(normalizeDriveUrl(u));
            updateGallery(norm);
        } else {
            String collection = firstNonNullExtra(EXTRA_COLLECTION, "collection", "coll", "type");
            String docId = firstNonNullExtra(EXTRA_DOCID, "docId", "doc_id", "id", "documentId");
            if (collection != null && docId != null) {
                Log.d(TAG, "Prefetch gallery from Firestore: " + collection + "/" + docId);
                fetchImagesFromFirestore(collection, docId);
            } else {
                ArrayList<String> imgsFromIntent = getIntent().getStringArrayListExtra("images");
                if (imgsFromIntent != null && !imgsFromIntent.isEmpty()) {
                    ArrayList<String> norm = new ArrayList<>();
                    for (String u : imgsFromIntent) norm.add(normalizeDriveUrl(u));
                    updateGallery(norm);
                } else {
                    String main = firstNonNullExtra("imageUrl", "image", "mainImage", "photo");
                    if (main != null && !main.trim().isEmpty()) {
                        ArrayList<String> tmp = new ArrayList<>();
                        tmp.add(normalizeDriveUrl(main.trim()));
                        updateGallery(tmp);
                    } else {
                        Log.d(TAG, "No gallery source found");
                    }
                }
            }
        }
    }

    private String firstNonNullExtra(String... keys) {
        Intent intent = getIntent();
        if (intent == null) return null;
        for (String k : keys) {
            if (intent.hasExtra(k)) {
                String v = intent.getStringExtra(k);
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return null;
    }

    private void bindWisata(Object wisata, String extraImageUrl) {
        String nama = safeGetString(wisata, "getNama");
        String lokasi = safeGetString(wisata, "getLokasi");
        String deskripsi = safeGetString(wisata, "getDeskripsi");
        Double lat = safeGetDouble(wisata, "getLatitude");
        Double lon = safeGetDouble(wisata, "getLongitude");

        tvNama.setText(nama != null ? nama : "-");
        tvLokasi.setText(lokasi != null ? lokasi : "-");
        tvDeskripsi.setText(deskripsi != null ? deskripsi : "");
        tvExtra.setText("");

        if (extraImageUrl == null || extraImageUrl.trim().isEmpty()) loadImageFromModel(wisata);

        Double maybeRating = safeGetDouble(wisata, "getRating");
        if (maybeRating != null && maybeRating > 0) setRating(maybeRating);
        else { ratingBar.setVisibility(View.GONE); tvRatingValue.setVisibility(View.GONE); }
        tvPrice.setVisibility(View.GONE);

        setupLocationClick(lokasi, lat, lon);
    }

    private void bindKuliner(Object kuliner, String extraImageUrl) {
        String nama = safeGetString(kuliner, "getNama");
        String lokasi = safeGetString(kuliner, "getLokasi");
        String deskripsi = safeGetString(kuliner, "getDeskripsi");
        String catatan = safeGetString(kuliner, "getCatatan");
        Double rating = safeGetDouble(kuliner, "getRating");
        Double lat = safeGetDouble(kuliner, "getLatitude");
        Double lon = safeGetDouble(kuliner, "getLongitude");

        tvNama.setText(nama != null ? nama : "-");
        tvLokasi.setText(lokasi != null ? lokasi : "-");
        tvDeskripsi.setText(deskripsi != null ? deskripsi : "");
        String extra = (catatan != null ? catatan : "");
        if (rating != null && rating > 0) extra += (extra.isEmpty() ? "" : "\n") + "Rating: " + rating;
        tvExtra.setText(extra);

        if (extraImageUrl == null || extraImageUrl.trim().isEmpty()) loadImageFromModel(kuliner);

        if (rating != null && rating > 0) setRating(rating);
        else { ratingBar.setVisibility(View.GONE); tvRatingValue.setVisibility(View.GONE); }
        tvPrice.setVisibility(View.GONE);

        setupLocationClick(lokasi, lat, lon);
    }

    private void bindPenginapan(Object penginapan, String extraImageUrl) {
        String nama = safeGetString(penginapan, "getNama");
        String alamat = safeGetString(penginapan, "getAlamat");
        String deskripsi = safeGetString(penginapan, "getDeskripsi");
        Double rating = safeGetDouble(penginapan, "getRating");
        Integer harga = safeGetInt(penginapan, "getHargaPerMalam");
        Double lat = safeGetDouble(penginapan, "getLatitude");
        Double lon = safeGetDouble(penginapan, "getLongitude");

        tvNama.setText(nama != null ? nama : "-");
        tvLokasi.setText(alamat != null ? alamat : "-");
        tvDeskripsi.setText(deskripsi != null ? deskripsi : "");

        String extra = "";
        if (rating != null) extra += "Rating: " + rating;
        if (harga != null && harga > 0) {
            if (!extra.isEmpty()) extra += "  |  ";
            extra += "Harga: Rp " + formatPrice(harga);
        }
        tvExtra.setText(extra);

        if (extraImageUrl == null || extraImageUrl.trim().isEmpty()) loadImageFromModel(penginapan);

        if (rating != null && rating > 0) setRating(rating);
        else { ratingBar.setVisibility(View.GONE); tvRatingValue.setVisibility(View.GONE); }

        if (harga != null && harga > 0) tvPrice.setText("Rp " + formatPrice(harga));
        else tvPrice.setText("");

        setupLocationClick(alamat, lat, lon);
    }

    private void setRating(double rating) {
        ratingBar.setVisibility(View.VISIBLE);
        tvRatingValue.setVisibility(View.VISIBLE);
        ratingBar.setRating((float) rating);
        tvRatingValue.setText(String.valueOf(rating));
    }

    private void setupLocationClick(String lokasiText, Double latitude, Double longitude) {
        final String label = (lokasiText != null) ? lokasiText : "";
        tvLokasi.setClickable(true);
        tvLokasi.setOnClickListener(v -> openMap(latitude, longitude, label));
        btnOpenMaps.setOnClickListener(v -> openMap(latitude, longitude, label));
    }

    private void openMap(Double lat, Double lon, String label) {
        try {
            Intent intent;
            if (lat != null && lon != null) {
                String uri = "geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + Uri.encode(label) + ")";
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            } else if (label != null && !label.trim().isEmpty()) {
                String uri = "geo:0,0?q=" + Uri.encode(label);
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            } else {
                Toast.makeText(this, "Lokasi tidak tersedia.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
            else {
                String url;
                if (lat != null && lon != null) url = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                else url = "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(label);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        } catch (Exception e) {
            Log.w(TAG, "openMap failed: " + e.getMessage(), e);
            Toast.makeText(this, "Gagal membuka peta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // load image utama from model if model exposes list or string
    private void loadImageFromModel(Object model) {
        if (model == null) {
            ivGambar.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        try {
            Method m = model.getClass().getMethod("getImageUrl");
            Object res = m.invoke(model);
            if (res instanceof String) {
                String url = (String) res;
                if (url != null && !url.trim().isEmpty()) {
                    loadImage(url);
                    return;
                }
            } else if (res instanceof List) {
                List<?> l = (List<?>) res;
                if (!l.isEmpty()) {
                    ArrayList<String> arr = new ArrayList<>();
                    for (Object o : l) if (o != null) arr.add(normalizeDriveUrl(String.valueOf(o)));
                    Log.d(TAG, "loadImageFromModel found list size=" + arr.size());
                    updateGallery(arr);
                    loadImage(arr.get(0));
                    return;
                }
            }
        } catch (NoSuchMethodException ignored) {} catch (Exception e) {
            Log.w(TAG, "error calling getImageUrl() on model: " + e.getMessage(), e);
        }
        ivGambar.setImageResource(R.drawable.ic_image_placeholder);
    }

    private void loadImage(String url) {
        if (url == null || url.trim().isEmpty()) {
            ivGambar.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }
        String normalized = normalizeDriveUrl(url);
        Log.d(TAG, "loadImage -> normalized: " + normalized);
        Glide.with(this)
                .load(normalized)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .into(ivGambar);
    }

    private String safeGetString(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object r = m.invoke(obj);
            return r != null ? String.valueOf(r) : null;
        } catch (Exception ignored) {}
        return null;
    }
    private Double safeGetDouble(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object r = m.invoke(obj);
            if (r instanceof Number) return ((Number) r).doubleValue();
            if (r instanceof String) { try { return Double.parseDouble(((String) r).trim()); } catch (NumberFormatException ignored) {} }
        } catch (Exception ignored) {}
        return null;
    }
    private Integer safeGetInt(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object r = m.invoke(obj);
            if (r instanceof Number) return ((Number) r).intValue();
            if (r instanceof String) { try { return Integer.parseInt(((String) r).trim()); } catch (NumberFormatException ignored) {} }
        } catch (Exception ignored) {}
        return null;
    }

    private ArrayList<String> getImageListFromModel(Object model) {
        ArrayList<String> out = new ArrayList<>();
        if (model == null) return out;
        try {
            String[] methodNames = {"getImages","getGallery","getImageUrls","getPhotos","getGalleries","getFotoList","getImagesList"};
            for (String mn : methodNames) {
                try {
                    Method m = model.getClass().getMethod(mn);
                    Object r = m.invoke(model);
                    if (r instanceof List) {
                        for (Object o : (List<?>) r) if (o != null) out.add(String.valueOf(o));
                        if (!out.isEmpty()) return out;
                    }
                } catch (NoSuchMethodException ignored) {}
            }
            Field[] fields = model.getClass().getDeclaredFields();
            for (Field f : fields) {
                try {
                    f.setAccessible(true);
                    Object val = f.get(model);
                    if (val instanceof List) {
                        for (Object o : (List<?>) val) if (o != null) out.add(String.valueOf(o));
                        if (!out.isEmpty()) return out;
                    }
                } catch (Exception ignored) {}
            }
            for (int i=1;i<=6;i++){
                try {
                    Method mi = model.getClass().getMethod("getImage"+i);
                    Object r = mi.invoke(model);
                    if (r != null) out.add(String.valueOf(r));
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Exception e) {
            Log.w(TAG, "getImageListFromModel error: " + e.getMessage(), e);
        }
        return out;
    }

    private String normalizeDriveUrl(String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.contains("drive.google.com/uc?export=view")) return input;
        Pattern p = Pattern.compile("/d/([a-zA-Z0-9_-]+)");
        Matcher m = p.matcher(input);
        if (m.find()) return "https://drive.google.com/uc?export=view&id=" + m.group(1);
        p = Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)");
        m = p.matcher(input);
        if (m.find()) return "https://drive.google.com/uc?export=view&id=" + m.group(1);
        p = Pattern.compile("open\\?id=([a-zA-Z0-9_-]+)");
        m = p.matcher(input);
        if (m.find()) return "https://drive.google.com/uc?export=view&id=" + m.group(1);
        return input;
    }

    // Robust fetch that normalizes all URLs and logs everything
    private void fetchImagesFromFirestore(@NonNull String collection, @NonNull String docId) {
        Log.d(TAG, "fetchImagesFromFirestore -> collection=" + collection + " docId=" + docId);
        firestore.collection(collection).document(docId)
                .get()
                .addOnSuccessListener((DocumentSnapshot documentSnapshot) -> {
                    Log.d(TAG, "Firestore fetched: exists=" + documentSnapshot.exists());
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(DetailActivity.this, "Data tidak ditemukan.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayList<String> urls = new ArrayList<>();

                    // TRY typed List
                    try {
                        List<?> typed = documentSnapshot.get("imageUrl", List.class);
                        if (typed != null && !typed.isEmpty()) {
                            Log.d(TAG, "imageUrl typed List size=" + typed.size());
                            for (Object o : typed) {
                                if (o == null) continue;
                                String s = String.valueOf(o).trim();
                                if (!s.isEmpty()) urls.add(normalizeDriveUrl(s));
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "typed List retrieval failed: " + e.getMessage());
                    }

                    // fallback raw parsing
                    if (urls.isEmpty()) {
                        Object raw = documentSnapshot.get("imageUrl");
                        Log.d(TAG, "raw imageUrl class=" + (raw != null ? raw.getClass().getSimpleName() : "null") + " value=" + String.valueOf(raw));
                        if (raw instanceof List) {
                            for (Object o : (List<?>) raw) {
                                if (o == null) continue;
                                String s = String.valueOf(o).trim();
                                if (!s.isEmpty()) urls.add(normalizeDriveUrl(s));
                            }
                        } else if (raw instanceof String) {
                            String s = ((String) raw).trim();
                            if (s.startsWith("[") && s.endsWith("]")) {
                                try {
                                    JSONArray ja = new JSONArray(s);
                                    for (int k = 0; k < ja.length(); k++) {
                                        String u = ja.optString(k, null);
                                        if (u != null && !u.trim().isEmpty()) urls.add(normalizeDriveUrl(u));
                                    }
                                    Log.d(TAG, "parsed JSON-string array count=" + urls.size());
                                } catch (JSONException je) {
                                    urls.add(normalizeDriveUrl(s));
                                }
                            } else {
                                urls.add(normalizeDriveUrl(s));
                            }
                        }
                    }

                    // try 'images' field as last fallback
                    if (urls.isEmpty()) {
                        Object imagesField = documentSnapshot.get("images");
                        Log.d(TAG, "raw images field class=" + (imagesField != null ? imagesField.getClass().getSimpleName() : "null"));
                        if (imagesField instanceof List) {
                            for (Object o : (List<?>) imagesField) {
                                if (o == null) continue;
                                String s = String.valueOf(o).trim();
                                if (!s.isEmpty()) urls.add(normalizeDriveUrl(s));
                            }
                        } else if (imagesField instanceof String) {
                            String s = ((String) imagesField).trim();
                            if (s.startsWith("[") && s.endsWith("]")) {
                                try {
                                    JSONArray ja = new JSONArray(s);
                                    for (int k = 0; k < ja.length(); k++) {
                                        String u = ja.optString(k, null);
                                        if (u != null && !u.trim().isEmpty()) urls.add(normalizeDriveUrl(u));
                                    }
                                } catch (JSONException ignored) {
                                    urls.add(normalizeDriveUrl(s));
                                }
                            } else {
                                urls.add(normalizeDriveUrl(s));
                            }
                        }
                    }

                    Log.d(TAG, "urls collected before dedup size=" + urls.size());
                    for (int idx = 0; idx < urls.size(); idx++) Log.d(TAG, "urls[" + idx + "] = " + urls.get(idx));

                    // Di dalam onAddOnSuccessListener, bagian akhir setelah dedup:
                    if (!urls.isEmpty()) {
                        // dedup preserve order
                        Set<String> dedup = new LinkedHashSet<>(urls);
                        ArrayList<String> finalList = new ArrayList<>(dedup);
                        Log.d(TAG, "finalList size after dedup=" + finalList.size());

                        // GANTI INI: updateGallery(finalList);
                        // MENJADI:
                        updateGalleryForTab(finalList);
                    } else {
                        if (recyclerGallery != null) recyclerGallery.setVisibility(View.GONE);
                        Toast.makeText(DetailActivity.this, "Tidak ada foto untuk galeri.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(DetailActivity.this, "Gagal ambil galeri: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "fetchImagesFromFirestore failed", e);
                });
    }

    private void updateGallery(ArrayList<String> urls) {
        Log.d(TAG, "updateGallery called with urls.size=" + (urls != null ? urls.size() : 0));
        if (recyclerGallery == null || galleryAdapter == null) {
            Log.w(TAG, "updateGallery: recyclerGallery or adapter null");
            return;
        }

        runOnUiThread(() -> {
            // Normalisasi semua URLs
            ArrayList<String> normalized = new ArrayList<>();
            if (urls != null) {
                for (String u : urls) normalized.add(normalizeDriveUrl(u));
            }

            // Update adapter (tapi jangan ubah visibility karena ini prefetch)
            galleryAdapter.setItems(normalized);

            Log.d(TAG, "updateGallery: data loaded, adapter.getItemCount() = " + galleryAdapter.getItemCount());
            // JANGAN ubah visibility di sini karena default view adalah Info tab
        });
    }

    private void showInfoTab() {
        if (recyclerGallery != null) recyclerGallery.setVisibility(View.GONE);
        View info = findViewById(R.id.infoContainer);
        if (info != null) info.setVisibility(View.VISIBLE);
        View reviews = findViewById(R.id.reviewsContainer);
        if (reviews != null) reviews.setVisibility(View.GONE);
    }

    private void showGalleryTab() {
        // Cek apakah sudah ada data di adapter
        if (galleryAdapter != null && galleryAdapter.getItemCount() > 0) {
            // Sudah ada data, langsung tampilkan
            recyclerGallery.setVisibility(View.VISIBLE);
            View info = findViewById(R.id.infoContainer);
            if (info != null) info.setVisibility(View.GONE);
            View reviews = findViewById(R.id.reviewsContainer);
            if (reviews != null) reviews.setVisibility(View.GONE);
            Log.d(TAG, "showGalleryTab: displaying " + galleryAdapter.getItemCount() + " images");
            return;
        }

        // Belum ada data, coba fetch
        Log.d(TAG, "showGalleryTab: no data yet, attempting fetch");

        // Coba dari model dulu
        ArrayList<String> fromModel = getImageListFromModel(currentModel);
        if (!fromModel.isEmpty()) {
            Log.d(TAG, "showGalleryTab: found images in model: " + fromModel.size());
            ArrayList<String> norm = new ArrayList<>();
            for (String u : fromModel) norm.add(normalizeDriveUrl(u));
            updateGalleryForTab(norm);
            return;
        }

        // Coba dari Firestore
        String collection = firstNonNullExtra(EXTRA_COLLECTION, "collection", "coll", "type");
        String docId = firstNonNullExtra(EXTRA_DOCID, "docId", "doc_id", "id", "documentId");
        if (collection != null && docId != null) {
            Log.d(TAG, "showGalleryTab: fetching from Firestore");
            fetchImagesFromFirestore(collection, docId);
        } else {
            // Fallback ke gambar utama jika ada
            String main = firstNonNullExtra("imageUrl", "image", "mainImage", "photo");
            if (main != null && !main.trim().isEmpty()) {
                ArrayList<String> tmp = new ArrayList<>();
                tmp.add(normalizeDriveUrl(main.trim()));
                updateGalleryForTab(tmp);
            } else {
                Toast.makeText(this, "Tidak ada foto untuk galeri.", Toast.LENGTH_SHORT).show();
                showInfoTab(); // kembali ke info
            }
        }
    }

    private void updateGalleryForTab(ArrayList<String> urls) {
        Log.d(TAG, "updateGalleryForTab called with urls.size=" + (urls != null ? urls.size() : 0));
        if (recyclerGallery == null || galleryAdapter == null) {
            Log.w(TAG, "updateGalleryForTab: recyclerGallery or adapter null");
            return;
        }

        runOnUiThread(() -> {
            ArrayList<String> normalized = new ArrayList<>();
            if (urls != null) {
                for (String u : urls) normalized.add(normalizeDriveUrl(u));
            }

            galleryAdapter.setItems(normalized);
            recyclerGallery.setVisibility(normalized.isEmpty() ? View.GONE : View.VISIBLE);

            // Sembunyikan container lain untuk tab gallery
            View info = findViewById(R.id.infoContainer);
            if (info != null) info.setVisibility(View.GONE);
            View reviews = findViewById(R.id.reviewsContainer);
            if (reviews != null) reviews.setVisibility(View.GONE);

            Log.d(TAG, "updateGalleryForTab: adapter.getItemCount() = " + galleryAdapter.getItemCount());

            if (normalized.isEmpty()) {
                Toast.makeText(DetailActivity.this, "Tidak ada foto untuk galeri.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReviewsTab() {
        if (recyclerGallery != null) recyclerGallery.setVisibility(View.GONE);
        View info = findViewById(R.id.infoContainer);
        if (info != null) info.setVisibility(View.GONE);
        View reviews = findViewById(R.id.reviewsContainer);
        if (reviews != null) reviews.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Tab Ulasan belum diimplementasikan.", Toast.LENGTH_SHORT).show();
    }

    private String formatPrice(int price) {
        try {
            NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
            String s = nf.format(price);
            if (Locale.getDefault().getLanguage().equalsIgnoreCase("id")) s = s.replace(",", ".");
            return s;
        } catch (Exception ignored) {}
        return String.valueOf(price);
    }
}
