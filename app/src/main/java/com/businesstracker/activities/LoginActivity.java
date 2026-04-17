package com.businesstracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.businesstracker.R;
import com.businesstracker.databinding.ActivityLoginBinding;
import com.businesstracker.models.User;
import com.businesstracker.storage.StorageService;
import com.businesstracker.utils.TaskReminderWorker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.recaptcha.Recaptcha;
import com.google.android.recaptcha.RecaptchaAction;
import com.google.android.recaptcha.RecaptchaTasksClient;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private StorageService storage;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private RecaptchaTasksClient recaptchaTasksClient;
    private String generatedCode;

    // --- EMAILJS CONFIGURATION ---
    private static final String EMAILJS_SERVICE_ID = "service_gqav4y7"; 
    private static final String EMAILJS_TEMPLATE_ID = "template_4rer8wm";
    private static final String EMAILJS_PUBLIC_KEY = "jufAWBXYqPsioWkX5";
    private static final String EMAILJS_PRIVATE_KEY = "B58Wgw9DY3ZGwxmOOUI4C";
    // -----------------------------

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed: " + e.getStatusCode(), e);
                        Toast.makeText(this, "Google Sign-In Error: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        storage = StorageService.getInstance(this);
        mAuth = FirebaseAuth.getInstance();
        mAuth.useAppLanguage();

        initializeRecaptcha();
        setupWorkManager();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            User user = new User(currentUser.getUid(), currentUser.getEmail(), 
                    currentUser.getDisplayName(), currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
            storage.setUser(user);
            goToDashboard();
            return;
        }

        // Login UI
        binding.btnSignIn.setOnClickListener(v -> executeWithRecaptcha(RecaptchaAction.LOGIN, this::handleEmailLogin));
        binding.tvGoToSignUp.setOnClickListener(v -> showMode(Mode.SIGN_UP));
        binding.btnGoogle.setOnClickListener(v -> handleGoogleLogin()); 
        binding.tvForgotPassword.setOnClickListener(v -> showMode(Mode.RESET));

        // Sign Up UI
        binding.btnSignUp.setOnClickListener(v -> executeWithRecaptcha(RecaptchaAction.SIGNUP, this::handleEmailSignUp));
        binding.btnBackToSignInFromSignUp.setOnClickListener(v -> showMode(Mode.LOGIN));
        binding.btnGoogleSignUp.setOnClickListener(v -> handleGoogleLogin()); 

        // Reset & Verify
        binding.btnSendReset.setOnClickListener(v -> executeWithRecaptcha(RecaptchaAction.custom("send_reset_code"), this::handleRequestVerificationCode));
        binding.btnBackToSignIn.setOnClickListener(v -> showMode(Mode.LOGIN));
        binding.btnVerifyCode.setOnClickListener(v -> handleVerifyCode());
        binding.btnBackToReset.setOnClickListener(v -> showMode(Mode.RESET));
    }

    private enum Mode { LOGIN, SIGN_UP, RESET, VERIFY }

    private void showMode(Mode mode) {
        binding.layoutLogin.setVisibility(mode == Mode.LOGIN ? View.VISIBLE : View.GONE);
        binding.layoutSignUp.setVisibility(mode == Mode.SIGN_UP ? View.VISIBLE : View.GONE);
        binding.layoutReset.setVisibility(mode == Mode.RESET ? View.VISIBLE : View.GONE);
        binding.layoutVerifyCode.setVisibility(mode == Mode.VERIFY ? View.VISIBLE : View.GONE);
    }

    private void setupWorkManager() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();

        PeriodicWorkRequest reminderRequest =
                new PeriodicWorkRequest.Builder(TaskReminderWorker.class, 1, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "TaskReminders",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
        );
    }

    private void initializeRecaptcha() {
        Recaptcha.getTasksClient(getApplication(), "6LddTJssAAAAACe7y2o-glQGPKrdW8-KkHBGszFu")
                .addOnSuccessListener(client -> this.recaptchaTasksClient = client)
                .addOnFailureListener(e -> Log.e(TAG, "reCAPTCHA failed: " + e.getMessage()));
    }

    private void executeWithRecaptcha(RecaptchaAction action, Runnable onSuccess) {
        if (recaptchaTasksClient == null) {
            onSuccess.run();
            return;
        }

        recaptchaTasksClient.executeTask(action)
                .addOnSuccessListener(token -> onSuccess.run())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "reCAPTCHA error: " + e.getMessage());
                    onSuccess.run();
                });
    }

    private void handleEmailLogin() {
        String email    = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            storage.clearAllData(); 
                            User user = new User(firebaseUser.getUid(), firebaseUser.getEmail(), 
                                    firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : email.split("@")[0], null);
                            storage.setUser(user);
                            goToDashboard();
                        }
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleEmailSignUp() {
        String name     = binding.etSignUpName.getText().toString().trim();
        String email    = binding.etSignUpEmail.getText().toString().trim();
        String password = binding.etSignUpPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            storage.clearAllData(); 
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            firebaseUser.updateProfile(profileUpdates).addOnCompleteListener(t -> {
                                User user = new User(firebaseUser.getUid(), firebaseUser.getEmail(), name, null);
                                storage.setUser(user);
                                goToDashboard();
                            });
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleGoogleLogin() {
        googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            storage.clearAllData();
                            User user = new User(firebaseUser.getUid(), firebaseUser.getEmail(), 
                                    firebaseUser.getDisplayName(), firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : null);
                            storage.setUser(user);
                            goToDashboard();
                        }
                    } else {
                        Toast.makeText(this, "Google auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleRequestVerificationCode() {
        String email = binding.etResetEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedCode = String.format("%06d", new Random().nextInt(999999));
        sendVerificationViaEmailJS(email, generatedCode);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> Log.d(TAG, "Firebase reset email requested"));

        Toast.makeText(this, "Identity check... Code sent to email.", Toast.LENGTH_SHORT).show();
        showMode(Mode.VERIFY);
    }

    private void sendVerificationViaEmailJS(String toEmail, String code) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject templateParams = new JSONObject();
                templateParams.put("to_email", toEmail);
                templateParams.put("code", code);
                templateParams.put("message", "Your verification code is: " + code);

                JSONObject jsonBody = new JSONObject();
                jsonBody.put("service_id", EMAILJS_SERVICE_ID);
                jsonBody.put("template_id", EMAILJS_TEMPLATE_ID);
                jsonBody.put("user_id", EMAILJS_PUBLIC_KEY);
                jsonBody.put("accessToken", EMAILJS_PRIVATE_KEY);
                jsonBody.put("template_params", templateParams);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonBody.toString().getBytes("utf-8"));
                }
                conn.getResponseCode();
            } catch (Exception e) {
                Log.e(TAG, "EmailJS Error", e);
            }
        }).start();
    }

    private void handleVerifyCode() {
        String enteredCode = binding.etVerifyCode.getText().toString().trim();
        if (generatedCode != null && enteredCode.equals(generatedCode)) {
            new AlertDialog.Builder(this)
                    .setTitle("Verified")
                    .setMessage("Check your email (including spam) for the Firebase reset link.")
                    .setPositiveButton("OK", (dialog, which) -> showMode(Mode.LOGIN))
                    .show();
        } else {
            Toast.makeText(this, "Incorrect code", Toast.LENGTH_SHORT).show();
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
