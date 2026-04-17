package com.businesstracker.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.businesstracker.R;
import com.businesstracker.adapters.ExpenseAdapter;
import com.businesstracker.adapters.SaleAdapter;
import com.businesstracker.adapters.TaskAdapter;
import com.businesstracker.models.Business;
import com.businesstracker.models.Expense;
import com.businesstracker.models.Sale;
import com.businesstracker.models.Task;
import com.businesstracker.storage.StorageService;
import com.businesstracker.utils.CalendarServiceHelper;
import com.businesstracker.utils.DriveServiceHelper;
import com.businesstracker.utils.PdfGenerator;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.tabs.TabLayout;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class BusinessDetailActivity extends AppCompatActivity implements StorageService.OnDataChangedListener {

    public static final String EXTRA_BUSINESS_ID = "businessId";
    private static final String TAG = "BusinessDetailActivity";

    private StorageService storage;
    private String businessId;
    private Business business;

    private TextView tvName, tvDescription, tvBudget, tvSpent, tvRevenue, tvProfit;
    private TextView tvProgress, tvStatus, tvEmptyState;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TabLayout tabLayout;
    private MaterialButton fabAdd, btnExport;
    private View colorDotDetail;
    private LinearLayout layoutTableHeader;
    private TextView tvCol1, tvCol2, tvCol3, tvCol4, tvCol5;

    private int currentTab = 0; // 0 = expenses, 1 = tasks, 2 = sales

    // Google Services Helpers
    private DriveServiceHelper mDriveServiceHelper;
    private CalendarServiceHelper mCalendarServiceHelper;

    // For photo handling
    private String encodedPhoto = null;
    private ImageView ivPreview;
    private ImageButton btnRemove;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            setExpensePhoto(imageBitmap);
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        setExpensePhoto(bitmap);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to load image", e);
                        Toast.makeText(this, R.string.failed_to_load_image, Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> driveAuthLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    handleExport(); // Retry export after authorization
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_detail);

        storage = StorageService.getInstance(this);
        storage.addOnDataChangedListener(this);
        businessId = getIntent().getStringExtra(EXTRA_BUSINESS_ID);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvName        = findViewById(R.id.tvBusinessName);
        tvDescription = findViewById(R.id.tvDescription);
        tvBudget      = findViewById(R.id.tvBudget);
        tvSpent       = findViewById(R.id.tvSpent);
        tvRevenue     = findViewById(R.id.tvRevenue);
        tvProfit      = findViewById(R.id.tvProfit);
        tvProgress    = findViewById(R.id.tvProgressPercent);
        tvStatus      = findViewById(R.id.tvStatus);
        tvEmptyState  = findViewById(R.id.tvEmptyState);
        progressBar   = findViewById(R.id.progressBar);
        recyclerView  = findViewById(R.id.recyclerView);
        tabLayout     = findViewById(R.id.tabLayout);
        fabAdd        = findViewById(R.id.fabAdd);
        btnExport     = findViewById(R.id.btnExport);
        colorDotDetail = findViewById(R.id.colorDotDetail);
        layoutTableHeader = findViewById(R.id.layoutTableHeader);
        tvCol1 = findViewById(R.id.tvCol1);
        tvCol2 = findViewById(R.id.tvCol2);
        tvCol3 = findViewById(R.id.tvCol3);
        tvCol4 = findViewById(R.id.tvCol4);
        tvCol5 = findViewById(R.id.tvCol5);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                refreshList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        fabAdd.setOnClickListener(v -> {
            if (currentTab == 0) showAddExpenseDialog();
            else if (currentTab == 1) showAddTaskDialog();
            else showAddSaleDialog();
        });

        btnExport.setOnClickListener(v -> handleExport());

        findViewById(R.id.btnSettings).setOnClickListener(v -> showEditBusinessDialog());

        initializeGoogleServices();
        loadData();
    }

    @Override
    public void onNotificationChanged() {
        runOnUiThread(this::loadData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        storage.removeOnDataChangedListener(this);
    }

    private void initializeGoogleServices() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && account.getAccount() != null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this, Arrays.asList(DriveScopes.DRIVE_FILE, CalendarScopes.CALENDAR));
            credential.setSelectedAccount(account.getAccount());

            // Initialize Drive
            Drive googleDriveService = new Drive.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("Business Tracker")
                    .build();
            mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

            // Initialize Calendar
            Calendar calendarService = new Calendar.Builder(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    credential)
                    .setApplicationName("Business Tracker")
                    .build();
            mCalendarServiceHelper = new CalendarServiceHelper(calendarService);
        }
    }

    private void handleExport() {
        if (mDriveServiceHelper == null) {
            Toast.makeText(this, R.string.google_sign_in_required_export, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.generating_pdf_uploading, Toast.LENGTH_SHORT).show();

        executorService().execute(() -> {
            try {
                List<Expense> expenses = new ArrayList<>();
                for (Expense e : storage.getExpenses()) {
                    if (e.getBusinessId().equals(businessId)) expenses.add(e);
                }

                List<Task> tasks = new ArrayList<>();
                for (Task t : storage.getTasks()) {
                    if (t.getBusinessId().equals(businessId)) tasks.add(t);
                }

                File pdfFile = PdfGenerator.generateBusinessReport(this, business, expenses, tasks);

                mDriveServiceHelper.uploadFile(pdfFile, "application/pdf", null)
                        .addOnSuccessListener(fileId -> runOnUiThread(() -> Toast.makeText(this, R.string.report_uploaded_drive, Toast.LENGTH_LONG).show()))
                        .addOnFailureListener(exception -> runOnUiThread(() -> {
                            if (exception instanceof UserRecoverableAuthIOException) {
                                driveAuthLauncher.launch(((UserRecoverableAuthIOException) exception).getIntent());
                            } else {
                                Toast.makeText(this, getString(R.string.upload_failed_prefix) + exception.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }));

            } catch (Exception e) {
                Log.e(TAG, "Export failed", e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_failed_prefix) + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private java.util.concurrent.Executor executorService() {
        return java.util.concurrent.Executors.newSingleThreadExecutor();
    }

    private void setExpensePhoto(Bitmap bitmap) {
        if (ivPreview != null) {
            ivPreview.setImageBitmap(bitmap);
            ivPreview.setPadding(0, 0, 0, 0);
            if (btnRemove != null) btnRemove.setVisibility(View.VISIBLE);
            
            // Encode to Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] bytes = baos.toByteArray();
            encodedPhoto = Base64.encodeToString(bytes, Base64.DEFAULT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        List<Business> all = storage.getBusinesses();
        business = null;
        for (Business b : all) {
            if (b.getId().equals(businessId)) { business = b; break; }
        }
        if (business == null) { finish(); return; }
        bindBusiness();
        refreshList();
    }

    private void bindBusiness() {
        tvName.setText(business.getName());
        tvDescription.setText(business.getDescription());
        tvDescription.setVisibility(business.getDescription().isEmpty() ? View.GONE : View.VISIBLE);

        tvBudget.setText(String.format(Locale.US, "₱%,.0f", business.getTargetBudget()));
        tvSpent.setText(String.format(Locale.US, "₱%,.0f", business.getCurrentSpent()));
        tvRevenue.setText(String.format(Locale.US, "₱%,.2f", business.getTotalRevenue()));
        
        double profit = business.getProfit();
        tvProfit.setText(String.format(Locale.US, "₱%,.2f", profit));
        
        if (business.getTotalRevenue() == 0 && business.getCurrentSpent() == 0) {
            tvProfit.setTextColor(Color.GRAY);
        } else if (profit >= 0) {
            tvProfit.setTextColor(Color.parseColor("#10b981")); // Green for profit
        } else {
            tvProfit.setTextColor(Color.parseColor("#EF4444")); // Red for loss
        }

        double pct = business.getProgressPercent();
        progressBar.setProgress((int) Math.min(pct, 100));
        tvProgress.setText(String.format(Locale.US, "%.1f%% of budget used", pct));

        // Apply custom business color
        int bColor = Color.parseColor(business.getColor());
        colorDotDetail.setBackgroundTintList(ColorStateList.valueOf(bColor));
        progressBar.setProgressTintList(ColorStateList.valueOf(bColor));
        tabLayout.setSelectedTabIndicatorColor(bColor);

        if (business.isOverBudget()) {
            tvStatus.setText(R.string.status_over_budget);
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFEF4444")));
            tvStatus.setTextColor(Color.WHITE);
        } else if (business.isNearBudget()) {
            tvStatus.setText(R.string.status_near_limit);
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFF97316")));
            tvStatus.setTextColor(Color.WHITE);
        } else {
            tvStatus.setText(R.string.status_on_track);
            tvStatus.setBackgroundTintList(ColorStateList.valueOf(bColor));
            tvStatus.setTextColor(Color.WHITE);
        }

        if (getSupportActionBar() != null) getSupportActionBar().setTitle(business.getName());
    }

    private void refreshList() {
        if (currentTab == 0) showExpenses();
        else if (currentTab == 1) showTasks();
        else showSales();
    }

    private void showExpenses() {
        layoutTableHeader.setVisibility(View.VISIBLE);
        tvCol1.setText(R.string.header_date);
        tvCol2.setText(R.string.header_type);
        tvCol3.setText(R.string.header_description);
        tvCol4.setVisibility(View.VISIBLE);
        tvCol4.setText(R.string.header_photo);
        tvCol4.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.8f));
        tvCol5.setVisibility(View.VISIBLE);
        tvCol5.setText(R.string.header_amount);

        List<Expense> all = storage.getExpenses();
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : all) {
            if (e.getBusinessId().equals(businessId)) filtered.add(e);
        }
        if (filtered.isEmpty()) {
            tvEmptyState.setText(R.string.empty_expenses);
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            layoutTableHeader.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        recyclerView.setAdapter(new ExpenseAdapter(filtered, new ExpenseAdapter.OnExpenseActionListener() {
            @Override
            public void onDelete(String id) {
                new AlertDialog.Builder(BusinessDetailActivity.this)
                        .setTitle(R.string.dialog_delete_expense_title)
                        .setMessage(R.string.dialog_delete_expense_message)
                        .setPositiveButton(R.string.action_delete, (d, w) -> {
                            storage.deleteExpense(id);
                            loadData();
                            Toast.makeText(BusinessDetailActivity.this, R.string.toast_expense_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }

            @Override
            public void onEdit(Expense expense) {
                showEditExpenseDialog(expense);
            }

            @Override
            public void onPhotoClick(Bitmap photo) {
                showFullPhoto(photo);
            }
        }));
    }

    private void showFullPhoto(Bitmap bitmap) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_photo);
        
        ImageView ivFull = dialog.findViewById(R.id.ivFullPhoto);
        ivFull.setImageBitmap(bitmap);

        View btnDownload = dialog.findViewById(R.id.btnDownloadPhoto);
        if (btnDownload != null) {
            btnDownload.setOnClickListener(v -> saveImageToGallery(bitmap));
        }

        View btnClose = dialog.findViewById(R.id.btnCloseFull);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String filename = "Expense_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BusinessTracker");
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    Toast.makeText(this, R.string.photo_saved_gallery, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save photo", e);
                Toast.makeText(this, R.string.failed_to_save_photo, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTasks() {
        layoutTableHeader.setVisibility(View.VISIBLE);
        tvCol1.setText(R.string.header_done);
        tvCol2.setText(R.string.header_title);
        tvCol3.setText(R.string.header_description);
        tvCol4.setVisibility(View.VISIBLE);
        tvCol4.setText(R.string.header_due_date);
        tvCol4.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f));
        tvCol5.setVisibility(View.GONE);

        List<Task> all = storage.getTasks();
        List<Task> filtered = new ArrayList<>();
        for (Task t : all) {
            if (t.getBusinessId().equals(businessId)) filtered.add(t);
        }
        if (filtered.isEmpty()) {
            tvEmptyState.setText(R.string.empty_tasks);
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            layoutTableHeader.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        recyclerView.setAdapter(new TaskAdapter(filtered, new TaskAdapter.OnTaskActionListener() {
            @Override
            public void onToggle(String id, boolean checked) {
                List<Task> tasks = storage.getTasks();
                for (Task t : tasks) {
                    if (t.getId().equals(id)) { 
                        t.setCompleted(checked); 
                        storage.updateTask(id, t); 
                        break; 
                    }
                }
                loadData();
            }

            @Override
            public void onDelete(String id) {
                new AlertDialog.Builder(BusinessDetailActivity.this)
                        .setTitle(R.string.dialog_delete_task_title)
                        .setMessage(R.string.dialog_delete_task_message)
                        .setPositiveButton(R.string.action_delete, (d, w) -> {
                            storage.deleteTask(id);
                            loadData();
                            Toast.makeText(BusinessDetailActivity.this, R.string.toast_task_deleted, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            }

            @Override
            public void onEdit(Task task) {
                showEditTaskDialog(task);
            }
        }));
    }

    private void showSales() {
        layoutTableHeader.setVisibility(View.GONE); 
        List<Sale> all = storage.getSales();
        List<Sale> filtered = new ArrayList<>();
        for (Sale s : all) {
            if (s.getBusinessId().equals(businessId)) filtered.add(s);
        }
        if (filtered.isEmpty()) {
            tvEmptyState.setText(R.string.empty_sales);
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        recyclerView.setAdapter(new SaleAdapter(filtered, id -> new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_sale_title)
                .setMessage(R.string.dialog_delete_sale_message)
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    storage.deleteSale(id);
                    loadData();
                    Toast.makeText(this, R.string.toast_sale_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show()));
    }

    private void showAddExpenseDialog() {
        encodedPhoto = null;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);
        EditText etType   = view.findViewById(R.id.etType);
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etDate   = view.findViewById(R.id.etDate);
        EditText etDesc   = view.findViewById(R.id.etDescription);
        
        ivPreview = view.findViewById(R.id.ivExpensePhoto);
        btnRemove = view.findViewById(R.id.btnRemovePhoto);
        MaterialButton btnTake = view.findViewById(R.id.btnTakePhoto);
        MaterialButton btnUpload = view.findViewById(R.id.btnUploadPhoto);

        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.select_expense_date)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                etDate.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        btnTake.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        btnUpload.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnRemove.setOnClickListener(v -> {
            encodedPhoto = null;
            ivPreview.setImageResource(android.R.drawable.ic_menu_camera);
            ivPreview.setPadding(30, 30, 30, 30);
            btnRemove.setVisibility(View.GONE);
        });

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        etDate.setText(today);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_expense_title)
                .setView(view)
                .setPositiveButton(R.string.action_add, (d, w) -> {
                    String type   = etType.getText().toString().trim();
                    String amount = etAmount.getText().toString().trim();
                    String date   = etDate.getText().toString().trim();
                    String desc   = etDesc.getText().toString().trim();

                    if (type.isEmpty() || amount.isEmpty()) {
                        Toast.makeText(this, R.string.toast_required_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amt;
                    try { amt = Double.parseDouble(amount); } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.toast_invalid_amount, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Expense expense = new Expense(
                            String.valueOf(System.currentTimeMillis()),
                            businessId, type, amt, date, desc);
                    expense.setPhoto(encodedPhoto);
                    storage.addExpense(expense);
                    loadData();
                    Toast.makeText(this, R.string.toast_expense_added, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEditExpenseDialog(Expense expense) {
        encodedPhoto = expense.getPhoto();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null);
        EditText etType   = view.findViewById(R.id.etType);
        EditText etAmount = view.findViewById(R.id.etAmount);
        EditText etDate   = view.findViewById(R.id.etDate);
        EditText etDesc   = view.findViewById(R.id.etDescription);
        
        ivPreview = view.findViewById(R.id.ivExpensePhoto);
        btnRemove = view.findViewById(R.id.btnRemovePhoto);
        MaterialButton btnTake = view.findViewById(R.id.btnTakePhoto);
        MaterialButton btnUpload = view.findViewById(R.id.btnUploadPhoto);

        etType.setText(expense.getType());
        etAmount.setText(String.valueOf(expense.getAmount()));
        etDate.setText(expense.getDate());
        etDesc.setText(expense.getDescription());

        if (encodedPhoto != null && !encodedPhoto.isEmpty()) {
            byte[] decodedString = Base64.decode(encodedPhoto, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            ivPreview.setImageBitmap(decodedByte);
            ivPreview.setPadding(0, 0, 0, 0);
            btnRemove.setVisibility(View.VISIBLE);
        }

        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.select_expense_date)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                etDate.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        btnTake.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        btnUpload.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnRemove.setOnClickListener(v -> {
            encodedPhoto = null;
            ivPreview.setImageResource(android.R.drawable.ic_menu_camera);
            ivPreview.setPadding(30, 30, 30, 30);
            btnRemove.setVisibility(View.GONE);
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_expense_title)
                .setView(view)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String type   = etType.getText().toString().trim();
                    String amount = etAmount.getText().toString().trim();
                    String date   = etDate.getText().toString().trim();
                    String desc   = etDesc.getText().toString().trim();

                    if (type.isEmpty() || amount.isEmpty()) return;

                    double newAmount = expense.getAmount();
                    try { newAmount = Double.parseDouble(amount); } catch (NumberFormatException ignored) {}

                    expense.setType(type);
                    expense.setAmount(newAmount);
                    expense.setDate(date);
                    expense.setDescription(desc);
                    expense.setPhoto(encodedPhoto);

                    storage.deleteExpense(expense.getId()); 
                    storage.addExpense(expense); 
                    
                    loadData();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAddTaskDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etTitle   = view.findViewById(R.id.etTitle);
        EditText etDesc    = view.findViewById(R.id.etTaskDescription);
        EditText etDueDate = view.findViewById(R.id.etDueDate);

        etDueDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.select_due_date)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                etDueDate.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_task_title)
                .setView(view)
                .setPositiveButton(R.string.action_add, (d, w) -> {
                    String title   = etTitle.getText().toString().trim();
                    String desc    = etDesc.getText().toString().trim();
                    String dueDate = etDueDate.getText().toString().trim();

                    if (title.isEmpty()) {
                        Toast.makeText(this, R.string.toast_title_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Task task = new Task(
                            String.valueOf(System.currentTimeMillis()),
                            businessId, title, desc, dueDate, false, false);
                    storage.addTask(task);
                    loadData();

                    if (mCalendarServiceHelper != null) {
                        new AlertDialog.Builder(this)
                                .setTitle(R.string.dialog_add_calendar_title)
                                .setMessage(R.string.dialog_add_calendar_message)
                                .setPositiveButton(R.string.action_yes, (dialog, which) -> mCalendarServiceHelper.createEvent(title, desc, dueDate)
                                        .addOnSuccessListener(link -> Toast.makeText(this, R.string.toast_event_added_calendar, Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.failed_add_calendar_prefix) + e.getMessage(), Toast.LENGTH_SHORT).show()))
                                .setNegativeButton(R.string.action_no, null)
                                .show();
                    }

                    Toast.makeText(this, R.string.toast_task_added, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEditTaskDialog(Task task) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etTitle   = view.findViewById(R.id.etTitle);
        EditText etDesc    = view.findViewById(R.id.etTaskDescription);
        EditText etDueDate = view.findViewById(R.id.etDueDate);

        etTitle.setText(task.getTitle());
        etDesc.setText(task.getDescription());
        etDueDate.setText(task.getDueDate());

        etDueDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.select_due_date)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                etDueDate.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_task_title)
                .setView(view)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    task.setTitle(etTitle.getText().toString().trim());
                    task.setDescription(etDesc.getText().toString().trim());
                    task.setDueDate(etDueDate.getText().toString().trim());
                    storage.updateTask(task.getId(), task);
                    loadData();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAddSaleDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_sale, null);
        EditText etName = view.findViewById(R.id.etProductName);
        EditText etQty = view.findViewById(R.id.etSaleQuantity);
        EditText etPrice = view.findViewById(R.id.etSalePrice);
        EditText etTotal = view.findViewById(R.id.etTotalReceived);
        EditText etDate = view.findViewById(R.id.etSaleDate);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        etDate.setText(today);

        etDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.select_sale_date)
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                etDate.setText(sdf.format(new Date(selection)));
            });
            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        TextWatcher autoCalc = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    String qStr = etQty.getText().toString();
                    String pStr = etPrice.getText().toString();
                    if (!qStr.isEmpty() && !pStr.isEmpty()) {
                        double q = Double.parseDouble(qStr);
                        double p = Double.parseDouble(pStr);
                        etTotal.setText(String.format(Locale.US, "%.2f", q * p));
                    }
                } catch (Exception ignored) {}
            }
        };
        etQty.addTextChangedListener(autoCalc);
        etPrice.addTextChangedListener(autoCalc);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_add_sale_title)
                .setView(view)
                .setPositiveButton(R.string.action_add, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String qty = etQty.getText().toString().trim();
                    String price = etPrice.getText().toString().trim();
                    String totalStr = etTotal.getText().toString().trim();
                    String date = etDate.getText().toString().trim();

                    if (name.isEmpty() || totalStr.isEmpty()) {
                        Toast.makeText(this, R.string.toast_sale_required_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double totalAmount = Double.parseDouble(totalStr);
                        Sale sale = new Sale(String.valueOf(System.currentTimeMillis()), businessId, name, qty, price, totalAmount, date);
                        storage.addSale(sale);
                        loadData();
                        Toast.makeText(this, R.string.toast_sale_recorded, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.toast_invalid_total_amount, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showEditBusinessDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_business, null);
        EditText etName   = view.findViewById(R.id.etName);
        EditText etDesc   = view.findViewById(R.id.etDescription);
        EditText etBudget = view.findViewById(R.id.etBudget);
        EditText etRevenue = view.findViewById(R.id.etRevenue);
        EditText etQty    = view.findViewById(R.id.etSellQuantity);
        EditText etPrice  = view.findViewById(R.id.etUnitPrice);
        MaterialButtonToggleGroup toggleStatus = view.findViewById(R.id.toggleStatus);

        etName.setText(business.getName());
        etDesc.setText(business.getDescription());
        etBudget.setText(String.valueOf(business.getTargetBudget()));
        etRevenue.setText(String.valueOf(business.getTotalRevenue()));

        // Set current status
        if (business.getStatus().equalsIgnoreCase("now")) toggleStatus.check(R.id.btnStatusNow);
        else if (business.getStatus().equalsIgnoreCase("incoming")) toggleStatus.check(R.id.btnStatusIncoming);
        else if (business.getStatus().equalsIgnoreCase("done")) toggleStatus.check(R.id.btnStatusDone);

        TextWatcher autoCalc = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    String qStr = etQty.getText().toString();
                    String pStr = etPrice.getText().toString();
                    if (!qStr.isEmpty() && !pStr.isEmpty()) {
                        double q = Double.parseDouble(qStr);
                        double p = Double.parseDouble(pStr);
                        etRevenue.setText(String.format(Locale.US, "%.2f", q * p));
                    }
                } catch (Exception ignored) {}
            }
        };
        etQty.addTextChangedListener(autoCalc);
        etPrice.addTextChangedListener(autoCalc);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_edit_business_title)
                .setView(view)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String name   = etName.getText().toString().trim();
                    String desc   = etDesc.getText().toString().trim();
                    String budget = etBudget.getText().toString().trim();
                    String revenue = etRevenue.getText().toString().trim();
                    int checkedId = toggleStatus.getCheckedButtonId();

                    if (name.isEmpty()) { Toast.makeText(this, R.string.toast_name_required, Toast.LENGTH_SHORT).show(); return; }
                    
                    double budgetVal = business.getTargetBudget();
                    try { budgetVal = Double.parseDouble(budget); } catch (NumberFormatException ignored) {}

                    double revenueVal = business.getTotalRevenue();
                    try { revenueVal = Double.parseDouble(revenue); } catch (NumberFormatException ignored) {}

                    business.setName(name);
                    business.setDescription(desc);
                    business.setTargetBudget(budgetVal);
                    business.setTotalRevenue(revenueVal);
                    
                    if (checkedId == R.id.btnStatusNow) business.setStatus("now");
                    else if (checkedId == R.id.btnStatusIncoming) business.setStatus("incoming");
                    else if (checkedId == R.id.btnStatusDone) business.setStatus("done");

                    storage.updateBusiness(businessId, business);
                    loadData();
                    Toast.makeText(this, R.string.toast_business_updated, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.action_delete_business, (d, w) -> new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_delete_business_title)
                        .setMessage(getString(R.string.dialog_delete_business_message_prefix) + business.getName() + getString(R.string.dialog_delete_business_message_suffix))
                        .setPositiveButton(R.string.action_delete, (d2, w2) -> {
                            storage.deleteBusiness(businessId);
                            finish();
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}
