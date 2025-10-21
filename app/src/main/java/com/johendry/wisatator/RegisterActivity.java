package com.johendry.wisatator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword;
    private Button btnRegister;
    private TextView tvLoginLink;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(LanguageSelectionActivity.PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(LanguageSelectionActivity.KEY_LANG, "id");
        Context context = LocaleHelper.setLocale(newBase, lang);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // find views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        // firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        // Basic validation
        if (username.isEmpty()) {
            etUsername.setError("Username harus diisi");
            etUsername.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email tidak valid");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password minimal 6 karakter");
            etPassword.requestFocus();
            return;
        }

        // disable tombol untuk mencegah double click
        btnRegister.setEnabled(false);
        btnRegister.setText("Mendaftarkan...");

        // create account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // simpan profile ke Firestore (opsional tapi berguna)
                            saveUserProfileToFirestore(user.getUid(), username, email,
                                    // onSuccess
                                    () -> sendVerificationAndFinish(user),
                                    // onFailure
                                    e -> {
                                        // walau simpan profile gagal, tetap kirim verifikasi
                                        sendVerificationAndFinish(user);
                                    });
                        } else {
                            // unexpected — user null
                            Toast.makeText(RegisterActivity.this, "Gagal mengambil user setelah registrasi.", Toast.LENGTH_SHORT).show();
                            resetRegisterButton();
                        }
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Registrasi gagal";
                        Toast.makeText(RegisterActivity.this, "Registrasi gagal: " + err, Toast.LENGTH_LONG).show();
                        resetRegisterButton();
                    }
                });
    }

    // kirim email verifikasi lalu signOut & pindah ke LoginActivity
    private void sendVerificationAndFinish(@NotNull FirebaseUser user) {
        user.sendEmailVerification()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(RegisterActivity.this,
                            "Pendaftaran sukses. Email verifikasi telah dikirim. Silakan cek inbox/spam.",
                            Toast.LENGTH_LONG).show();
                    // sign out supaya user tidak otomatis masuk sebelum verifikasi
                    mAuth.signOut();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Jika pengiriman verifikasi gagal, beri tahu user tapi tetap sign out
                    Toast.makeText(RegisterActivity.this,
                            "Registrasi berhasil tapi gagal kirim verifikasi: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                });
    }

    // Simpan data profil user ke Firestore (collection "users")
    // onSuccess/onFailure callbacks simple interfaces
    private void saveUserProfileToFirestore(String uid, String username, String email, Runnable onSuccess, OnFailureListener onFailure) {
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);
        data.put("email", email);
        data.put("createdAt", Timestamp.now());

        db.collection("users").document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    if (onFailure != null) onFailure.onFailure(e);
                });
    }

    private void resetRegisterButton() {
        btnRegister.setEnabled(true);
        btnRegister.setText("Daftar");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
