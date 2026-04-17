package com.businesstracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.businesstracker.R;
import com.businesstracker.storage.StorageService;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            StorageService storage = StorageService.getInstance(this);

            if (storage.getUser() != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                // Biometric removed - always go to Dashboard if logged in
                startActivity(new Intent(this, MainActivity.class));
            } else {
                storage.setUser(null);
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 2000);
    }
}
