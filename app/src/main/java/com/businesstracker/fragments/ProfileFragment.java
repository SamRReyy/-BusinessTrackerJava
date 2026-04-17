package com.businesstracker.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.businesstracker.R;
import com.businesstracker.activities.LoginActivity;
import com.businesstracker.adapters.BusinessAdapter;
import com.businesstracker.models.Business;
import com.businesstracker.models.Expense;
import com.businesstracker.models.Sale;
import com.businesstracker.models.Task;
import com.businesstracker.models.User;
import com.businesstracker.storage.StorageService;
import com.businesstracker.utils.DriveServiceHelper;
import com.businesstracker.utils.PdfGenerator;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private StorageService storage;
    private User user;

    private TextView tvInitial, tvName, tvEmail;
    private ImageView ivAvatar;
    private TextView tvBizBadge, tvExpBadge, tvTaskBadge;
    private TextView tvDataBizCount, tvDataExpenseCount, tvDataTaskCount;

    private LinearLayout tabProfile, tabSettings, tabData, tabPrivacy, tabHelp;
    private ImageButton btnNavProfile, btnNavSettings, btnNavData, btnNavPrivacy, btnNavHelp;

    private DriveServiceHelper mDriveServiceHelper;
    private final ActivityResultLauncher<Intent> driveAuthLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    exportData();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        storage = StorageService.getInstance(requireContext());
        user    = storage.getUser();

        // Initialize Views
        tvInitial     = view.findViewById(R.id.tvInitial);
        ivAvatar      = view.findViewById(R.id.ivAvatar);
        tvName        = view.findViewById(R.id.tvProfileNameSummary);
        tvEmail       = view.findViewById(R.id.tvProfileEmailSummary);
        tvBizBadge    = view.findViewById(R.id.tvBizBadge);
        tvExpBadge    = view.findViewById(R.id.tvExpBadge);
        tvTaskBadge   = view.findViewById(R.id.tvTaskBadge);

        tvDataBizCount     = view.findViewById(R.id.tvDataBizCount);
        tvDataExpenseCount = view.findViewById(R.id.tvDataExpenseCount);
        tvDataTaskCount    = view.findViewById(R.id.tvDataTaskCount);

        tabProfile  = view.findViewById(R.id.tabContentProfile);
        tabSettings = view.findViewById(R.id.tabContentSettings);
        tabData     = view.findViewById(R.id.tabContentData);
        tabPrivacy  = view.findViewById(R.id.tabContentPrivacy);
        tabHelp     = view.findViewById(R.id.tabContentHelp);

        btnNavProfile  = view.findViewById(R.id.btnNavProfileTab);
        btnNavSettings = view.findViewById(R.id.btnNavSettingsTab);
        btnNavData     = view.findViewById(R.id.btnNavDataTab);
        btnNavPrivacy  = view.findViewById(R.id.btnNavPrivacyTab);
        btnNavHelp     = view.findViewById(R.id.btnNavHelpTab);

        setupNavigation();
        bindUserHeader();
        setupProfileTab(view);
        setupSettingsTab(view);
        setupDataTab(view);
        setupPrivacyTab(view);
        setupHelpTab(view);

        initializeGoogleServices();

        int startTab = 0;
        if (getArguments() != null) {
            startTab = getArguments().getInt("start_tab", 0);
        }
        showTab(startTab);
    }

    private void initializeGoogleServices() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
        if (account != null && account.getAccount() != null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), Collections.singletonList(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            Drive googleDriveService = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("Business Tracker")
                    .build();
            mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
        }
    }

    private void setupNavigation() {
        btnNavProfile.setOnClickListener(v -> showTab(0));
        btnNavSettings.setOnClickListener(v -> showTab(1));
        btnNavData.setOnClickListener(v -> showTab(2));
        btnNavPrivacy.setOnClickListener(v -> showTab(3));
        btnNavHelp.setOnClickListener(v -> showTab(4));
    }

    private void bindUserHeader() {
        if (user != null) {
            if (tvName != null) tvName.setText(user.getName());
            if (tvEmail != null) tvEmail.setText(user.getEmail());

            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                if (ivAvatar != null) {
                    ivAvatar.setVisibility(View.VISIBLE);
                    if (tvInitial != null) tvInitial.setVisibility(View.GONE);
                    Glide.with(this).load(user.getAvatar()).into(ivAvatar);
                }
            } else {
                if (ivAvatar != null) ivAvatar.setVisibility(View.GONE);
                if (tvInitial != null) {
                    tvInitial.setVisibility(View.VISIBLE);
                    tvInitial.setText(user.getInitial());
                }
            }
        }
        int biz = storage.getBusinesses().size();
        int exp = storage.getExpenses().size();
        int tasks = storage.getTasks().size();
        
        if (tvBizBadge != null) tvBizBadge.setText(biz + " Businesses");
        if (tvExpBadge != null) tvExpBadge.setText(exp + " Expenses");
        
        long done = 0;
        List<Task> taskList = storage.getTasks();
        for (Task t : taskList) if (t.isCompleted()) done++;
        if (tvTaskBadge != null) tvTaskBadge.setText(done + "/" + tasks + " Tasks Done");

        if (tvDataBizCount != null) tvDataBizCount.setText(String.valueOf(biz));
        if (tvDataExpenseCount != null) tvDataExpenseCount.setText(String.valueOf(exp));
        if (tvDataTaskCount != null) tvDataTaskCount.setText(String.valueOf(tasks));
    }

    private void setupProfileTab(View root) {
        EditText etName    = root.findViewById(R.id.etProfileName);
        EditText etEmail   = root.findViewById(R.id.etProfileEmail);
        Button   btnSave   = root.findViewById(R.id.btnSaveProfile);

        if (user != null) {
            if (etName != null) etName.setText(user.getName());
            if (etEmail != null) etEmail.setText(user.getEmail());
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                if (user != null) {
                    user.setName(etName.getText().toString().trim());
                    user.setEmail(etEmail.getText().toString().trim());
                    storage.setUser(user);
                    bindUserHeader();
                    Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupSettingsTab(View root) {
        SwitchMaterial swDarkMode = root.findViewById(R.id.swDarkMode);
        if (swDarkMode != null) {
            swDarkMode.setChecked(storage.isDarkMode());
            swDarkMode.setOnCheckedChangeListener((b, checked) -> {
                storage.setDarkMode(checked);
                AppCompatDelegate.setDefaultNightMode(
                        checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        View btnCurrency = root.findViewById(R.id.btnChangeCurrency);
        TextView tvCurrency = root.findViewById(R.id.tvCurrentCurrency);
        if (tvCurrency != null) tvCurrency.setText("Currently: " + storage.getCurrency());
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> showCurrencyDialog(tvCurrency));
        }

        SwitchMaterial swCloudSync = root.findViewById(R.id.swCloudSync);
        if (swCloudSync != null) {
            swCloudSync.setChecked(storage.isCloudSyncEnabled());
            swCloudSync.setOnCheckedChangeListener((b, checked) -> storage.setCloudSyncEnabled(checked));
        }
        
        SwitchMaterial swEmail = root.findViewById(R.id.swEmailNotif);
        if (swEmail != null) {
            swEmail.setChecked(storage.isEmailNotifEnabled());
            swEmail.setOnCheckedChangeListener((b, checked) -> {
                storage.setEmailNotifEnabled(checked);
                Toast.makeText(requireContext(), checked ? "Email notifications enabled" : "Email notifications disabled", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showCurrencyDialog(TextView tvDisplay) {
        String[] currencies = {"₱", "$", "€", "£", "¥", "₹"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Currency")
                .setItems(currencies, (dialog, which) -> {
                    String selected = currencies[which];
                    storage.setCurrency(selected);
                    if (tvDisplay != null) tvDisplay.setText("Currently: " + selected);
                    Toast.makeText(requireContext(), "Currency updated to " + selected, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void setupPrivacyTab(View root) {
        SwitchMaterial swHideValues = root.findViewById(R.id.swHideValues);
        if (swHideValues != null) {
            swHideValues.setChecked(storage.isHideValuesEnabled());
            swHideValues.setOnCheckedChangeListener((b, checked) -> {
                storage.setHideValuesEnabled(checked);
                Toast.makeText(requireContext(), checked ? "Privacy mode enabled" : "Privacy mode disabled", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupDataTab(View root) {
        Button btnExport = root.findViewById(R.id.btnExportData);
        Button btnClear  = root.findViewById(R.id.btnClearData);
        Button btnArchive = root.findViewById(R.id.btnArchive);

        if (btnExport != null) btnExport.setOnClickListener(v -> exportData());
        if (btnArchive != null) btnArchive.setOnClickListener(v -> showArchivedDialog());
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                    .setTitle("Danger Zone")
                    .setMessage("Clear all local data? This cannot be undone.")
                    .setPositiveButton("Clear Everything", (d, w) -> {
                        storage.clearAllData();
                        if (isAdded()) {
                            bindUserHeader();
                            Toast.makeText(requireContext(), "All data cleared", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show());
        }
    }

    private void showArchivedDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_archived_list, null);
        RecyclerView rv = view.findViewById(R.id.rvArchived);
        TextView tvEmpty = view.findViewById(R.id.tvNoArchived);

        List<Business> all = storage.getBusinesses();
        List<Business> archived = new ArrayList<>();
        for (Business b : all) if (b.isArchived()) archived.add(b);

        if (archived.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            
            rv.setAdapter(new BusinessAdapter(archived, new BusinessAdapter.OnBusinessClickListener() {
                @Override
                public void onClick(Business business) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Manage Archived")
                            .setMessage("Choose action for \"" + business.getName() + "\"")
                            .setPositiveButton("Restore", (d, w) -> {
                                storage.archiveBusiness(business.getId(), false);
                                showArchivedDialog();
                                bindUserHeader();
                            })
                            .setNeutralButton("Delete Permanently", (d, w) -> {
                                storage.deleteBusiness(business.getId());
                                showArchivedDialog();
                                bindUserHeader();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }

                @Override
                public void onLongClick(Business business) {}
            }));
        }

        new AlertDialog.Builder(requireContext())
                .setView(view)
                .setPositiveButton("Close", null)
                .show();
    }

    private void setupHelpTab(View root) {
        Button btnLogout = root.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogout());
        }
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            storage.setUser(null);
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private void showTab(int index) {
        tabProfile.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabSettings.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabData.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabPrivacy.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        tabHelp.setVisibility(index == 4 ? View.VISIBLE : View.GONE);

        updateNavButtons(index);
    }

    private void updateNavButtons(int activeIndex) {
        ImageButton[] buttons = {btnNavProfile, btnNavSettings, btnNavData, btnNavPrivacy, btnNavHelp};
        
        int activeIconColor = ContextCompat.getColor(requireContext(), R.color.text_on_card_primary);
        int inactiveIconColor = ContextCompat.getColor(requireContext(), R.color.text_on_card_secondary);
        int activeBgColor = ContextCompat.getColor(requireContext(), R.color.divider);

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] == null) continue;
            if (i == activeIndex) {
                buttons[i].setBackgroundResource(R.drawable.bg_badge_rounded);
                buttons[i].setBackgroundTintList(ColorStateList.valueOf(activeBgColor));
                buttons[i].setImageTintList(ColorStateList.valueOf(activeIconColor));
            } else {
                buttons[i].setBackgroundResource(android.R.color.transparent);
                buttons[i].setImageTintList(ColorStateList.valueOf(inactiveIconColor));
            }
        }
    }

    private void exportData() {
        if (mDriveServiceHelper == null) {
            Toast.makeText(requireContext(), "Google sign-in required for export", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(requireContext(), "Generating PDF & Uploading...", Toast.LENGTH_SHORT).show();

        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                List<Business> businesses = storage.getBusinesses();
                List<Expense> expenses = storage.getExpenses();
                List<Task> tasks = storage.getTasks();
                List<Sale> sales = storage.getSales();

                File pdfFile = PdfGenerator.generateFullReport(requireContext(), businesses, expenses, tasks, sales);

                mDriveServiceHelper.uploadFile(pdfFile, "application/pdf", null)
                        .addOnSuccessListener(fileId -> {
                            Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> Toast.makeText(activity, "Report uploaded to Google Drive", Toast.LENGTH_LONG).show());
                            }
                        })
                        .addOnFailureListener(exception -> {
                            Activity activity = getActivity();
                            if (activity != null) {
                                activity.runOnUiThread(() -> {
                                    if (exception instanceof UserRecoverableAuthIOException) {
                                        driveAuthLauncher.launch(((UserRecoverableAuthIOException) exception).getIntent());
                                    } else {
                                        Toast.makeText(activity, "Upload failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });

            } catch (Exception e) {
                Log.e(TAG, "Full Export failed", e);
                Activity activity = getActivity();
                if (activity != null) {
                    activity.runOnUiThread(() -> Toast.makeText(activity, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}
