package com.johendry.wisatator;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Language selection activity.
 * Fix: always call super.onCreate(...) first to avoid SuperNotCalledException.
 */
public class LanguageSelectionActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "app_prefs";
    public static final String KEY_LANG = "pref_lang"; // "id" atau "en"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // << harus selalu dipanggil paling awal

        // ambil bahasa yang tersimpan (jika ada)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANG, null);
        if (lang != null) {
            // apply locale sebelum navigasi/menampilkan UI
            LocaleHelper.setLocale(this, lang);
            navigateAfterLangSelection();
            return; // aman, karena super.onCreate sudah dipanggil
        }

        // belum memilih bahasa -> tampilkan layar pilihan
        setContentView(R.layout.activity_language_selection);

        Button btnId = findViewById(R.id.btnLangId);
        Button btnEn = findViewById(R.id.btnLangEn);

        btnId.setOnClickListener(v -> selectLanguage("id"));
        btnEn.setOnClickListener(v -> selectLanguage("en"));
    }

    private void selectLanguage(String lang) {
        // simpan preferensi
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();

        // apply locale sekarang juga
        LocaleHelper.setLocale(this, lang);

        // setelah pilih bahasa, navigasi sesuai session/auth
        navigateAfterLangSelection();
    }

    private void navigateAfterLangSelection() {
        // Cek FirebaseAuth terlebih dahulu (jika kamu pakai)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.isEmailVerified()) {
            // user sudah login & verified -> langsung ke MainActivity
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        // fallback pada SessionManager (SharedPreferences) jika kamu pakai session lokal
        SessionManager sm = new SessionManager(this);
        if (sm.isLoggedIn()) {
            // Meski SharedPreferences menunjukkan login, jika Firebase user null atau not verified,
            // lebih aman arahkan ke LoginActivity supaya token/verification fresh.
            Intent i = new Intent(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
            return;
        }

        // default -> ke LoginActivity
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }
}
