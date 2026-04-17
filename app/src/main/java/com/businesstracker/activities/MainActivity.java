package com.businesstracker.activities;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.businesstracker.R;
import com.businesstracker.adapters.NotificationAdapter;
import com.businesstracker.fragments.DashboardFragment;
import com.businesstracker.fragments.ProfileFragment;
import com.businesstracker.models.AppNotification;
import com.businesstracker.models.User;
import com.businesstracker.storage.StorageService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements StorageService.OnDataChangedListener {

    private TextView tvNotificationBadge, tvSelectionCount, tvActivityLogTitle;
    private DrawerLayout drawerLayout;
    private RecyclerView recyclerNotifications;
    private LinearLayout emptyNotificationView, layoutSelectionActions, layoutDefaultHeader;
    private NotificationAdapter notificationAdapter;
    private View btnMarkAllRead;
    private ImageButton btnSelectAll, btnDeleteSelected, btnMiniSelectAll, btnMiniDeleteAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply Dark Mode before super.onCreate
        boolean isDarkMode = StorageService.getInstance(this).isDarkMode();
        applyDarkMode(isDarkMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        drawerLayout = findViewById(R.id.drawerLayout);
        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        emptyNotificationView = findViewById(R.id.emptyNotificationView);
        
        // Headers
        layoutDefaultHeader = findViewById(R.id.layoutDefaultHeader);
        layoutSelectionActions = findViewById(R.id.layoutSelectionActions);
        
        tvSelectionCount = findViewById(R.id.tvSelectionCount);
        tvActivityLogTitle = findViewById(R.id.tvActivityLogTitle);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        
        // Multi-selection controls
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        
        // Mini controls
        btnMiniSelectAll = findViewById(R.id.btnMiniSelectAll);
        btnMiniDeleteAll = findViewById(R.id.btnMiniDeleteAll);

        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));

        StorageService.getInstance(this).addOnDataChangedListener(this);
        
        // START SYNC FROM FIRESTORE
        StorageService.getInstance(this).syncDataFromFirestore();

        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

        findViewById(R.id.btnNavDashboard).setOnClickListener(v -> loadFragment(new DashboardFragment()));
        findViewById(R.id.btnNavProfile).setOnClickListener(v -> showProfilePopup(v));
        
        // Notifications
        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            updateNotificationDrawer();
            drawerLayout.openDrawer(GravityCompat.END);
        });

        btnMarkAllRead.setOnClickListener(v -> {
            StorageService.getInstance(this).markAllNotificationsRead();
            updateNotificationBadge();
            updateNotificationDrawer();
            Toast.makeText(this, "All caught up!", Toast.LENGTH_SHORT).show();
        });

        // Mini Select All logic
        btnMiniSelectAll.setOnClickListener(v -> {
            updateNotificationDrawer(); // Ensure list is loaded
            if (notificationAdapter != null) {
                notificationAdapter.selectAll();
            }
        });

        // Mini Delete All (with confirmation implied by user request for "delete" button)
        btnMiniDeleteAll.setOnClickListener(v -> {
            List<AppNotification> current = StorageService.getInstance(this).getNotifications();
            if (!current.isEmpty()) {
                Set<String> allIds = new HashSet<>();
                for (AppNotification n : current) allIds.add(n.getId());
                StorageService.getInstance(this).deleteNotifications(allIds);
                Toast.makeText(this, "Cleared all activity logs", Toast.LENGTH_SHORT).show();
            }
        });

        // Multi-selection button listeners
        btnSelectAll.setOnClickListener(v -> {
            if (notificationAdapter != null) notificationAdapter.selectAll();
        });

        btnDeleteSelected.setOnClickListener(v -> {
            if (notificationAdapter != null) {
                Set<String> selectedIds = notificationAdapter.getSelectedIds();
                if (!selectedIds.isEmpty()) {
                    StorageService.getInstance(this).deleteNotifications(selectedIds);
                    notificationAdapter.clearSelection();
                    Toast.makeText(this, "Deleted " + selectedIds.size() + " logs", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateTopProfileIcon();
        updateNotificationBadge();
    }

    @Override
    public void onNotificationChanged() {
        runOnUiThread(() -> {
            updateNotificationBadge();
            updateNotificationDrawer();
        });
    }

    private void updateNotificationDrawer() {
        List<AppNotification> notifications = StorageService.getInstance(this).getNotifications();
        if (notifications.isEmpty()) {
            recyclerNotifications.setVisibility(View.GONE);
            emptyNotificationView.setVisibility(View.VISIBLE);
            setSelectionMode(false);
        } else {
            recyclerNotifications.setVisibility(View.VISIBLE);
            emptyNotificationView.setVisibility(View.GONE);
            
            if (notificationAdapter == null) {
                notificationAdapter = new NotificationAdapter(notifications, new NotificationAdapter.OnNotificationActionListener() {
                    @Override
                    public void onDelete(AppNotification notification) {
                        StorageService.getInstance(MainActivity.this).deleteNotification(notification.getId());
                    }

                    @Override
                    public void onSelectionModeChanged(boolean isSelectionMode) {
                        setSelectionMode(isSelectionMode);
                    }

                    @Override
                    public void onSelectionChanged(int count) {
                        tvSelectionCount.setText(count + " selected");
                    }
                });
                recyclerNotifications.setAdapter(notificationAdapter);
            } else {
                notificationAdapter.updateNotifications(notifications);
            }
        }
    }

    private void setSelectionMode(boolean isSelectionMode) {
        layoutSelectionActions.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        layoutDefaultHeader.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);
    }

    private void applyDarkMode(boolean enabled) {
        int targetMode = enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
        if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
            AppCompatDelegate.setDefaultNightMode(targetMode);
        }
    }

    private void updateNotificationBadge() {
        int count = StorageService.getInstance(this).getUnreadNotificationCount();
        if (count > 0) {
            tvNotificationBadge.setVisibility(View.VISIBLE);
            tvNotificationBadge.setText(String.valueOf(count));
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationBadge();
        updateTopProfileIcon();
    }

    private void updateTopProfileIcon() {
        User user = StorageService.getInstance(this).getUser();
        if (user != null) {
            TextView tvTopInitial = findViewById(R.id.tvTopInitial);
            ImageView ivTopAvatar = findViewById(R.id.ivTopAvatar);

            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                if (ivTopAvatar != null) {
                    ivTopAvatar.setVisibility(View.VISIBLE);
                    if (tvTopInitial != null) tvTopInitial.setVisibility(View.GONE);
                    Glide.with(this).load(user.getAvatar()).into(ivTopAvatar);
                }
            } else {
                if (ivTopAvatar != null) ivTopAvatar.setVisibility(View.GONE);
                if (tvTopInitial != null) {
                    tvTopInitial.setVisibility(View.VISIBLE);
                    tvTopInitial.setText(user.getInitial());
                }
            }
        }
    }

    private void showProfilePopup(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.layout_profile_popup, null);
        
        PopupWindow popupWindow = new PopupWindow(popupView, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(20);

        User user = StorageService.getInstance(this).getUser();
        if (user != null) {
            TextView tvName = popupView.findViewById(R.id.tvPopupName);
            TextView tvEmail = popupView.findViewById(R.id.tvPopupEmail);
            if (tvName != null) tvName.setText(user.getName());
            if (tvEmail != null) tvEmail.setText(user.getEmail());
        }

        popupView.findViewById(R.id.menuProfile).setOnClickListener(v -> {
            loadProfileWithTab(0);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuSettings).setOnClickListener(v -> {
            loadProfileWithTab(1);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuExport).setOnClickListener(v -> {
            loadProfileWithTab(2);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuPrivacy).setOnClickListener(v -> {
            loadProfileWithTab(3);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuHelp).setOnClickListener(v -> {
            loadProfileWithTab(4);
            popupWindow.dismiss();
        });

        popupView.findViewById(R.id.menuLogout).setOnClickListener(v -> {
            handleLogout();
            popupWindow.dismiss();
        });

        popupWindow.showAsDropDown(anchor, -200, 10);
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            StorageService.getInstance(this).setUser(null);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileWithTab(int tabIndex) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt("start_tab", tabIndex);
        fragment.setArguments(args);
        loadFragment(fragment);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void openBusinessDetail(String businessId) {
        Intent intent = new Intent(this, BusinessDetailActivity.class);
        intent.putExtra(BusinessDetailActivity.EXTRA_BUSINESS_ID, businessId);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            if (notificationAdapter != null && notificationAdapter.isSelectionMode()) {
                notificationAdapter.clearSelection();
            } else {
                drawerLayout.closeDrawer(GravityCompat.END);
            }
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StorageService.getInstance(this).removeOnDataChangedListener(this);
    }
}
