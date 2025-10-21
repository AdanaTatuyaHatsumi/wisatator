package com.johendry.wisatator;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnResendVerification;
    private TextView tvRegisterLink;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

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
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmailLogin);
        etPassword = findViewById(R.id.etPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        btnResendVerification = findViewById(R.id.btnResendVerification);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);

        mAuth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegisterLink.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        btnResendVerification.setOnClickListener(v -> resendVerification());
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email tidak valid");
            etEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password harus diisi");
            etPassword.requestFocus();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Masuk...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Masuk");

                    if (task.isSuccessful()) {
                        currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            if (currentUser.isEmailVerified()) {
                                // ✅ Email sudah diverifikasi
                                Toast.makeText(LoginActivity.this, "Login berhasil!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                // ❌ Email belum diverifikasi
                                Toast.makeText(LoginActivity.this,
                                        "Email belum diverifikasi. Silakan verifikasi dulu.",
                                        Toast.LENGTH_LONG).show();

                                btnResendVerification.setVisibility(View.VISIBLE);

                                mAuth.signOut();
                            }
                        }
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Login gagal";
                        Toast.makeText(LoginActivity.this, "Login gagal: " + err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void resendVerification() {
        if (currentUser != null) {
            currentUser.sendEmailVerification()
                    .addOnSuccessListener(unused -> Toast.makeText(LoginActivity.this,
                            "Email verifikasi sudah dikirim ulang. Cek inbox/spam.",
                            Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(LoginActivity.this,
                            "Gagal kirim ulang verifikasi: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
        } else {
            Toast.makeText(this, "Silakan login dulu untuk mengirim ulang verifikasi.", Toast.LENGTH_SHORT).show();
        }
    }
}
