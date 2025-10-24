package com.johendry.wisatator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";

    // UI
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    private RecyclerView recyclerWisata, recyclerKuliner, recyclerPenginapan, recyclerWisataSmall;
    private TextView tvGreeting;
    private Button btnLogout;

    // big wisata view
    private ImageView ivWisataBig;
    private TextView tvNamaWisataBig, tvDeskripsiWisataBig, tvLokasiWisataBig;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Adapters & lists
    private WisataAdapter wisataAdapter;
    private SmallWisataAdapter smallAdapter;
    private KulinerAdapter kulinerAdapter;
    private PenginapanAdapter penginapanAdapter;

    private final List<Wisata> wisataList = new ArrayList<>();
    private final List<Kuliner> kulinerList = new ArrayList<>();
    private final List<Penginapan> penginapanList = new ArrayList<>();

    // section containers
    private View sectionWisataContainer;
    private View sectionKulinerContainer;
    private View sectionPenginapanContainer;

    // language
    private String appLang = "id"; // default

    // -------------------- Activity lifecycle --------------------
    @Override
    protected void attachBaseContext(Context newBase) {
        // apply saved locale BEFORE onCreate so resources.resolve use correct locale
        SharedPreferences prefs = newBase.getSharedPreferences(LanguageSelectionActivity.PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(LanguageSelectionActivity.KEY_LANG, "id");
        Context ctx = LocaleHelper.setLocale(newBase, lang);
        super.attachBaseContext(ctx);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // read chosen language (for firestore localization logic)
        SharedPreferences prefs = getSharedPreferences(LanguageSelectionActivity.PREFS_NAME, MODE_PRIVATE);
        appLang = prefs.getString(LanguageSelectionActivity.KEY_LANG, "id");

        // Firebase auth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        // optional: enforce email verification
        if (!currentUser.isEmailVerified()) {
            Toast.makeText(this, getString(R.string.email_belum_terverifikasi), Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // toolbar + drawer
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer);
        if (drawerLayout != null) {
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        }

        NavigationView navView = findViewById(R.id.navigation_view);

// inflate footer (wrap_content)
        View footer = getLayoutInflater().inflate(R.layout.nav_footer, navView, false);

// set layout params supaya nempel di bawah tanpa menimpa menu
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        footer.setLayoutParams(params);

// tambahkan footer ke NavigationView
        navView.addView(footer);

// handler klik logout
        TextView tvLogout = footer.findViewById(R.id.nav_footer_logout);
        tvLogout.setOnClickListener(v -> {
            // contoh: clear session
            mAuth.signOut();
            // if you maintain custom session manager, clear it
            try { new SessionManager(this).logout(); } catch (Exception ignored) {}
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });

        // section containers
        sectionWisataContainer = findViewById(R.id.section_wisata_container);
        sectionKulinerContainer = findViewById(R.id.section_kuliner_container);
        sectionPenginapanContainer = findViewById(R.id.section_penginapan_container);

        // main views
        //recyclerWisata = findViewById(R.id.recyclerWisata); // if exists in layout
        recyclerKuliner = findViewById(R.id.recyclerKuliner);
        recyclerPenginapan = findViewById(R.id.recyclerPenginapan);
        recyclerWisataSmall = findViewById(R.id.recyclerWisataSmall);

        tvGreeting = findViewById(R.id.tvGreeting);
        //btnLogout = findViewById(R.id.btnLogout);

        // big wisata views
        ivWisataBig = findViewById(R.id.ivWisataBig);
        tvNamaWisataBig = findViewById(R.id.tvNamaWisataBig);
        tvDeskripsiWisataBig = findViewById(R.id.tvDeskripsiWisataBig);
        tvLokasiWisataBig = findViewById(R.id.tvLokasiWisataBig);

        // layout managers
        if (recyclerWisata != null) recyclerWisata.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        if (recyclerKuliner != null) recyclerKuliner.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        if (recyclerPenginapan != null) recyclerPenginapan.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        if (recyclerWisataSmall != null) recyclerWisataSmall.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // adapters (init empty)
        wisataAdapter = new WisataAdapter(this, new ArrayList<>());
        if (recyclerWisata != null) recyclerWisata.setAdapter(wisataAdapter);

        smallAdapter = new SmallWisataAdapter(this, new ArrayList<>());
        if (recyclerWisataSmall != null) recyclerWisataSmall.setAdapter(smallAdapter);

        kulinerAdapter = new KulinerAdapter(this, new ArrayList<>());
        if (recyclerKuliner != null) recyclerKuliner.setAdapter(kulinerAdapter);

        penginapanAdapter = new PenginapanAdapter(this, new ArrayList<>());
        if (recyclerPenginapan != null) recyclerPenginapan.setAdapter(penginapanAdapter);

        // Firestore
        db = FirebaseFirestore.getInstance();

        // update nav menu strings to current locale (important)
        updateNavigationMenuTitles();

        // greeting
        showGreetingForUser(currentUser);

        // load data
        loadWisataFromFirestore();
        loadKulinerFromFirestore();
        loadPenginapanFromFirestore();

        // logout button near greeting (behaviour)
        //if (btnLogout != null) {
        //    btnLogout.setOnClickListener(v -> {
        //        mAuth.signOut();
        //        // if you maintain custom session manager, clear it
        //        try { new SessionManager(this).logout(); } catch (Exception ignored) {}
        //        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        //        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        //        startActivity(i);
        //        finish();
        //    });
        //}
    }

    // update navigation menu titles so they reflect locale resources
    private void updateNavigationMenuTitles() {
        if (navigationView == null) return;
        Menu menu = navigationView.getMenu();
        try {
            MenuItem mWisata = menu.findItem(R.id.nav_wisata);
            MenuItem mKuliner = menu.findItem(R.id.nav_kuliner);
            MenuItem mPenginapan = menu.findItem(R.id.nav_penginapan);
            MenuItem mAll = menu.findItem(R.id.nav_all);
            if (mWisata != null) mWisata.setTitle(getString(R.string.wisata));
            if (mKuliner != null) mKuliner.setTitle(getString(R.string.kuliner));
            if (mPenginapan != null) mPenginapan.setTitle(getString(R.string.penginapan));
            if (mAll != null) mAll.setTitle(getString(R.string.semua));
        } catch (Exception e) {
            Log.w(TAG, "updateNavigationMenuTitles failed: " + e.getMessage(), e);
        }
    }

    private void showGreetingForUser(@NotNull FirebaseUser user) {
        String fallback = user.getEmail() != null ? user.getEmail() : getString(R.string.pengguna_fallback);
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String username = doc.getString("username");
                        if (username != null && !username.isEmpty()) {
                            if (tvGreeting != null) tvGreeting.setText(getString(R.string.halo_user, username));
                            return;
                        }
                    }
                    if (tvGreeting != null) tvGreeting.setText(getString(R.string.halo_user, fallback));
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Gagal ambil profile user", e);
                    if (tvGreeting != null) tvGreeting.setText(getString(R.string.halo_user, fallback));
                });
    }

    // ------------------ Firestore loaders ------------------
    private void loadWisataFromFirestore() {
        db.collection("wisata").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                wisataList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Wisata w = new Wisata();
                    w.setId(doc.getId());
                    String nama = getLocalizedStringFromDoc(doc, "nama", appLang);
                    String deskripsi = getLocalizedStringFromDoc(doc, "deskripsi", appLang);
                    String lokasi = getLocalizedStringFromDoc(doc, "lokasi", appLang);
                    w.setNama(nama != null ? nama : "-");
                    w.setDeskripsi(deskripsi != null ? deskripsi : "");
                    w.setLokasi(lokasi != null ? lokasi : "");
                    String imageUrl = getLocalizedStringFromDoc(doc, "imageUrl", appLang);
                    if (imageUrl == null || imageUrl.trim().isEmpty()) imageUrl = doc.getString("imageUrl");
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) w.setImageUrl(normalizeDriveUrl(imageUrl.trim()));
                    else w.setGambarRes(0);
                    Double lat = getDoubleNullable(doc, "latitude");
                    Double lon = getDoubleNullable(doc, "longitude");
                    if (lat != null) w.setLatitude(lat);
                    if (lon != null) w.setLongitude(lon);
                    wisataList.add(w);
                }

                // update adapters and big view
                wisataAdapter.updateList(wisataList);

                if (!wisataList.isEmpty()) {
                    Wisata top = wisataList.get(0);
                    setBigWisata(top);

                    List<Wisata> smallList = new ArrayList<>();
                    for (int i = 1; i < wisataList.size(); i++) smallList.add(wisataList.get(i));
                    smallAdapter.updateList(smallList);
                } else {
                    if (sectionWisataContainer != null) sectionWisataContainer.setVisibility(View.GONE);
                }

            } else {
                Log.w(TAG, "Error getting wisata.", task.getException());
                Toast.makeText(MainActivity.this, getString(R.string.gagal_memuat_wisata), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setBigWisata(Wisata top) {
        if (top == null) return;
        if (tvNamaWisataBig != null) tvNamaWisataBig.setText(top.getNama() != null ? top.getNama() : "-");
        if (tvDeskripsiWisataBig != null) tvDeskripsiWisataBig.setText(top.getDeskripsi() != null ? top.getDeskripsi() : "");
        if (tvLokasiWisataBig != null) tvLokasiWisataBig.setText(top.getLokasi() != null ? top.getLokasi() : "");
        if (ivWisataBig != null) {
            String img = top.getImageUrl();
            if (img != null && !img.trim().isEmpty()) {
                Glide.with(this).load(normalizeDriveUrl(img)).placeholder(R.drawable.ic_launcher_background).error(R.drawable.ic_broken_image).centerCrop().into(ivWisataBig);
            } else if (top.getGambarRes() != 0) {
                ivWisataBig.setImageResource(top.getGambarRes());
            } else {
                ivWisataBig.setImageResource(R.drawable.ic_launcher_background);
            }

            View card = findViewById(R.id.cardWisataBig);
            if (card != null) {
                card.setOnClickListener(v -> {
                    Intent i = new Intent(MainActivity.this, DetailActivity.class);
                    i.putExtra("wisata", top);

                    // 🔥 TAMBAHKAN INI - kirim collection dan docId untuk fetch gallery
                    i.putExtra(DetailActivity.EXTRA_COLLECTION, "wisata");
                    i.putExtra(DetailActivity.EXTRA_DOCID, top.getId());

                    if (top.getImageUrl() != null) i.putExtra("imageUrl", normalizeDriveUrl(top.getImageUrl()));
                    startActivity(i);
                });
            }
        }
    }

    private void loadKulinerFromFirestore() {
        db.collection("kuliner").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                kulinerList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Kuliner k = new Kuliner();
                    k.setId(getIntSafe(doc, "id"));
                    k.setNama(getLocalizedStringFromDoc(doc,"nama",appLang));
                    k.setDeskripsi(getLocalizedStringFromDoc(doc,"deskripsi",appLang));
                    k.setLokasi(getLocalizedStringFromDoc(doc,"lokasi",appLang));
                    String imageUrl = getLocalizedStringFromDoc(doc,"imageUrl",appLang);
                    if (imageUrl == null || imageUrl.trim().isEmpty()) imageUrl = doc.getString("imageUrl");
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) k.setImageUrl(normalizeDriveUrl(imageUrl.trim()));
                    k.setRating(getDoubleSafe(doc,"rating"));
                    Double lat = getDoubleNullable(doc,"latitude");
                    Double lon = getDoubleNullable(doc,"longitude");
                    if (lat != null) k.setLatitude(lat);
                    if (lon != null) k.setLongitude(lon);
                    kulinerList.add(k);
                }
                kulinerAdapter.updateList(kulinerList);
            } else {
                Log.w(TAG, "Error getting kuliner.", task.getException());
                Toast.makeText(MainActivity.this, getString(R.string.gagal_memuat_kuliner), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPenginapanFromFirestore() {
        db.collection("penginapan").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                penginapanList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Penginapan p = new Penginapan();
                    p.setId(getIntSafe(doc,"id"));
                    p.setNama(getLocalizedStringFromDoc(doc,"nama",appLang));
                    p.setDeskripsi(getLocalizedStringFromDoc(doc,"deskripsi",appLang));
                    p.setAlamat(getLocalizedStringFromDoc(doc,"alamat",appLang));
                    String imageUrl = getLocalizedStringFromDoc(doc,"imageUrl",appLang);
                    if (imageUrl == null || imageUrl.trim().isEmpty()) imageUrl = doc.getString("imageUrl");
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) p.setImageUrl(normalizeDriveUrl(imageUrl.trim()));
                    p.setRating(getDoubleSafe(doc,"rating"));
                    p.setHargaPerMalam(getIntSafe(doc,"hargaPerMalam"));
                    Double lat = getDoubleNullable(doc,"latitude");
                    Double lon = getDoubleNullable(doc,"longitude");
                    if (lat != null) p.setLatitude(lat);
                    if (lon != null) p.setLongitude(lon);
                    penginapanList.add(p);
                }
                penginapanAdapter.updateList(penginapanList);
            } else {
                Log.w(TAG, "Error getting penginapan.", task.getException());
                Toast.makeText(MainActivity.this, getString(R.string.gagal_memuat_penginapan), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Navigation drawer item clicks
    @Override
    public boolean onNavigationItemSelected(@NotNull MenuItem item) {
        int id = item.getItemId();
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);

        if (id == R.id.nav_all) {
            showAllSections();
            return true;
        } else if (id == R.id.nav_wisata) {
            showOnlySection(sectionWisataContainer);
            if (toolbar != null) toolbar.setTitle(getString(R.string.wisata));
            return true;
        } else if (id == R.id.nav_kuliner) {
            showOnlySection(sectionKulinerContainer);
            if (toolbar != null) toolbar.setTitle(getString(R.string.kuliner));
            return true;
        } else if (id == R.id.nav_penginapan) {
            showOnlySection(sectionPenginapanContainer);
            if (toolbar != null) toolbar.setTitle(getString(R.string.penginapan));
            return true;
        }
        return false;
    }

    private void showOnlySection(View visible) {
        if (sectionWisataContainer != null) sectionWisataContainer.setVisibility(visible == sectionWisataContainer ? View.VISIBLE : View.GONE);
        if (sectionKulinerContainer != null) sectionKulinerContainer.setVisibility(visible == sectionKulinerContainer ? View.VISIBLE : View.GONE);
        if (sectionPenginapanContainer != null) sectionPenginapanContainer.setVisibility(visible == sectionPenginapanContainer ? View.VISIBLE : View.GONE);
    }

    private void showAllSections() {
        if (sectionWisataContainer != null) sectionWisataContainer.setVisibility(View.VISIBLE);
        if (sectionKulinerContainer != null) sectionKulinerContainer.setVisibility(View.VISIBLE);
        if (sectionPenginapanContainer != null) sectionPenginapanContainer.setVisibility(View.VISIBLE);
        if (toolbar != null) toolbar.setTitle(getString(R.string.app_name));
    }

    // ------------------ Helpers ------------------
    private String getLocalizedStringFromDoc(QueryDocumentSnapshot doc, String baseField, String lang) {
        if (doc == null) return null;
        String l = (lang == null) ? "id" : lang;
        String[] candidates;
        if ("en".equalsIgnoreCase(l)) {
            candidates = new String[]{ baseField + "_en", baseField, baseField + "_id" };
        } else {
            candidates = new String[]{ baseField + "_id", baseField, baseField + "_en" };
        }
        for (String f : candidates) {
            if (doc.contains(f)) {
                Object val = doc.get(f);
                if (val == null) continue;
                String s = String.valueOf(val);
                if (s != null && !s.trim().isEmpty()) return s;
            }
        }
        return null;
    }

    private double getDoubleSafe(QueryDocumentSnapshot doc, String field) {
        Object obj = doc.get(field);
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble(((String) obj).trim()); }
            catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }

    private int getIntSafe(QueryDocumentSnapshot doc, String field) {
        Object obj = doc.get(field);
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try { return Integer.parseInt(((String) obj).trim()); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private Double getDoubleNullable(QueryDocumentSnapshot doc, String field) {
        Object obj = doc.get(field);
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble(((String) obj).trim()); }
            catch (NumberFormatException ignored) {}
        }
        return null;
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

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        super.onBackPressed();
    }
}
