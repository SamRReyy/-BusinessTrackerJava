package com.businesstracker.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.businesstracker.R;
import com.businesstracker.activities.MainActivity;
import com.businesstracker.adapters.BusinessAdapter;
import com.businesstracker.databinding.FragmentDashboardBinding;
import com.businesstracker.models.Business;
import com.businesstracker.storage.StorageService;
import com.businesstracker.viewmodels.DashboardViewModel;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment implements StorageService.OnDataChangedListener {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private BusinessAdapter businessAdapter;
    private StorageService storage;

    private String currentStatusFilter = "all";
    private String searchQuery = "";
    private final String WEATHER_API_KEY = "57106924aed8ed512ccb1a3c8b400ace";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                    getLastLocationAndFetchWeather();
                } else {
                    fetchWeatherDataByCity("New York");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        storage = StorageService.getInstance(requireContext());
        storage.addOnDataChangedListener(this);
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        binding.recyclerBusinesses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.btnDashboardAdd.setOnClickListener(v -> showAddBusinessDialog());

        binding.toggleChartType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                binding.barChart.setVisibility(checkedId == R.id.btnBarChart ? View.VISIBLE : View.GONE);
                binding.pieChart.setVisibility(checkedId == R.id.btnPieChart ? View.VISIBLE : View.GONE);
                updateToggleUI(checkedId);
            }
        });

        setupFilterListeners();
        setupSearchListener();
        setupObservers();
        checkLocationPermission();
    }

    private void setupSearchListener() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().toLowerCase().trim();
                List<Business> current = viewModel.getBusinesses().getValue();
                if (current != null) refreshBusinessList(current);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupObservers() {
        viewModel.getBusinesses().observe(getViewLifecycleOwner(), businesses -> {
            refreshBusinessList(businesses);
            buildBarChart(businesses);
            buildPieChart(businesses);
            updateStatusBadges(businesses);
        });

        viewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            binding.tvExpensesCount.setText(String.valueOf(expenses.size()));
        });

        viewModel.getTotalBudget().observe(getViewLifecycleOwner(), budget -> updateOverallProgress());
        viewModel.getTotalSpent().observe(getViewLifecycleOwner(), spent -> updateOverallProgress());
        viewModel.getTotalRevenue().observe(getViewLifecycleOwner(), revenue -> updateOverallProgress());
    }

    private void updateOverallProgress() {
        Double budget = viewModel.getTotalBudget().getValue();
        Double spent = viewModel.getTotalSpent().getValue();
        Double revenue = viewModel.getTotalRevenue().getValue();
        
        if (budget == null || spent == null) return;

        binding.tvTotalBudget.setText(formatValue(budget, true));
        binding.tvTotalSpent.setText(formatValue(spent, true));
        
        if (revenue != null) {
            binding.tvTotalRevenue.setText(formatValue(revenue, false));
            double netProfit = revenue - spent;
            binding.tvTotalProfit.setText(formatValue(netProfit, false));
            
            if (revenue == 0 && spent == 0) {
                binding.tvTotalProfit.setTextColor(Color.GRAY);
            } else if (netProfit >= 0) {
                binding.tvTotalProfit.setTextColor(Color.parseColor("#10b981"));
            } else {
                binding.tvTotalProfit.setTextColor(Color.parseColor("#EF4444"));
            }
        }

        double overallPct = budget > 0 ? (spent / budget) * 100 : 0;
        binding.progressOverall.setProgress((int) Math.min(overallPct, 100));
        
        if (storage.isHideValuesEnabled()) {
            binding.tvOverallSpentLine.setText("•••• of •••• spent");
        } else {
            binding.tvOverallSpentLine.setText(String.format(Locale.US, "%s%,.0f of %s%,.0f spent", 
                    storage.getCurrency(), spent, storage.getCurrency(), budget));
        }
        binding.tvOverallPct.setText(String.format(Locale.US, "%.1f%% of total budget used", overallPct));
    }

    private String formatValue(double value, boolean whole) {
        if (storage.isHideValuesEnabled()) return "••••";
        String pattern = whole ? "%s%,.0f" : "%s%,.2f";
        return String.format(Locale.US, pattern, storage.getCurrency(), value);
    }

    @Override
    public void onNotificationChanged() {}

    @Override
    public void onSettingsChanged() {
        if (isAdded()) {
            updateOverallProgress();
            List<Business> current = viewModel.getBusinesses().getValue();
            if (current != null) refreshBusinessList(current);
        }
    }

    private void updateStatusBadges(List<Business> businesses) {
        int countAll = 0, countNow = 0, countIncoming = 0, countDone = 0;
        for (Business b : businesses) {
            if (b.isArchived()) continue;
            countAll++;
            if (b.getStatus().equalsIgnoreCase("now")) countNow++;
            else if (b.getStatus().equalsIgnoreCase("incoming")) countIncoming++;
            else if (b.getStatus().equalsIgnoreCase("done")) countDone++;
        }
        binding.badgeAll.setText(String.valueOf(countAll));
        binding.badgeNow.setText(String.valueOf(countNow));
        binding.badgeIncoming.setText(String.valueOf(countIncoming));
        binding.badgeDone.setText(String.valueOf(countDone));
    }

    private void updateToggleUI(int checkedId) {
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.text_on_card_primary);
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_on_card_secondary);

        if (checkedId == R.id.btnBarChart) {
            binding.btnBarChart.setTextColor(primaryColor);
            binding.btnBarChart.setTypeface(null, Typeface.BOLD);
            binding.btnPieChart.setTextColor(secondaryColor);
            binding.btnPieChart.setTypeface(null, Typeface.NORMAL);
        } else {
            binding.btnPieChart.setTextColor(primaryColor);
            binding.btnPieChart.setTypeface(null, Typeface.BOLD);
            binding.btnBarChart.setTextColor(secondaryColor);
            binding.btnBarChart.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocationAndFetchWeather();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void getLastLocationAndFetchWeather() {
        try {
            LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }
            }

            if (location != null) {
                fetchWeatherDataByCoords(location.getLatitude(), location.getLongitude());
            } else {
                fetchWeatherDataByCity("New York");
            }
        } catch (Exception e) {
            fetchWeatherDataByCity("New York");
        }
    }

    private void fetchWeatherDataByCoords(double lat, double lon) {
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s&units=metric", lat, lon, WEATHER_API_KEY);
        executeWeatherFetch(url);
    }

    private void fetchWeatherDataByCity(String city) {
        String url = String.format("https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric", city, WEATHER_API_KEY);
        executeWeatherFetch(url);
    }

    private void executeWeatherFetch(String urlString) {
        executor.execute(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(conn.getInputStream());
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    
                    String temp = String.format(Locale.getDefault(), "%.0f°C", json.get("main").getAsJsonObject().get("temp").getAsFloat());
                    String desc = json.get("weather").getAsJsonArray().get(0).getAsJsonObject().get("description").getAsString();
                    String cityName = json.get("name").getAsString();
                    String iconCode = json.get("weather").getAsJsonArray().get(0).getAsJsonObject().get("icon").getAsString();
                    String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                    mainHandler.post(() -> {
                        if (isAdded()) {
                            binding.tvWeatherTemp.setText(temp);
                            binding.tvWeatherDesc.setText(desc.substring(0, 1).toUpperCase() + desc.substring(1));
                            binding.tvWeatherCity.setText(cityName);
                            Glide.with(DashboardFragment.this).load(iconUrl).into(binding.imgWeatherIcon);
                        }
                    });
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (isAdded()) {
                        binding.tvWeatherDesc.setText("Weather unavailable");
                    }
                });
            }
        });
    }

    private void setupFilterListeners() {
        binding.filterAll.setOnClickListener(v -> updateFilter("all"));
        binding.filterNow.setOnClickListener(v -> updateFilter("now"));
        binding.filterIncoming.setOnClickListener(v -> updateFilter("incoming"));
        binding.filterDone.setOnClickListener(v -> updateFilter("done"));
    }

    private void updateFilter(String filter) {
        currentStatusFilter = filter;
        updateFilterUI();
        List<Business> current = viewModel.getBusinesses().getValue();
        if (current != null) refreshBusinessList(current);
    }

    private void updateFilterUI() {
        binding.filterAll.setBackground(null);
        binding.filterNow.setBackground(null);
        binding.filterIncoming.setBackground(null);
        binding.filterDone.setBackground(null);

        LinearLayout selected = null;
        if (currentStatusFilter.equals("all")) selected = binding.filterAll;
        else if (currentStatusFilter.equals("now")) selected = binding.filterNow;
        else if (currentStatusFilter.equals("incoming")) selected = binding.filterIncoming;
        else if (currentStatusFilter.equals("done")) selected = binding.filterDone;

        if (selected != null) {
            selected.setBackgroundResource(R.drawable.bg_nav_selected);
        }
    }

    private void refreshBusinessList(List<Business> businesses) {
        List<Business> filtered = new ArrayList<>();
        for (Business b : businesses) {
            if (b.isArchived()) continue;
            boolean matchesStatus = currentStatusFilter.equals("all") || b.getStatus().equalsIgnoreCase(currentStatusFilter);
            boolean matchesSearch = searchQuery.isEmpty() || b.getName().toLowerCase().contains(searchQuery);
            if (matchesStatus && matchesSearch) filtered.add(b);
        }

        businessAdapter = new BusinessAdapter(filtered, new BusinessAdapter.OnBusinessClickListener() {
            @Override
            public void onClick(Business business) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).openBusinessDetail(business.getId());
                }
            }

            @Override
            public void onLongClick(Business business) {
                showArchiveDialog(business);
            }
        });
        binding.recyclerBusinesses.setAdapter(businessAdapter);
    }

    private void showArchiveDialog(Business business) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Archive Business")
                .setMessage("Are you sure you want to archive \"" + business.getName() + "\"? It will be hidden from the dashboard.")
                .setPositiveButton("Archive", (d, w) -> {
                    storage.archiveBusiness(business.getId(), true);
                    Toast.makeText(requireContext(), "Business archived", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void buildBarChart(List<Business> businesses) {
        List<Business> active = new ArrayList<>();
        for (Business b : businesses) if (!b.isArchived()) active.add(b);
        
        if (active.isEmpty()) { binding.barChart.setVisibility(View.GONE); return; }
        binding.barChart.setVisibility(View.VISIBLE);
        
        List<BarEntry> budgetEntries = new ArrayList<>();
        List<BarEntry> spentEntries  = new ArrayList<>();
        List<String>   labels        = new ArrayList<>();

        for (int i = 0; i < active.size(); i++) {
            Business b = active.get(i);
            budgetEntries.add(new BarEntry(i, (float) b.getTargetBudget()));
            spentEntries.add(new BarEntry(i, (float) b.getCurrentSpent()));
            labels.add(b.getName());
        }

        BarDataSet budgetSet = new BarDataSet(budgetEntries, "Target Budget");
        budgetSet.setColor(Color.parseColor("#1e3a8a")); 
        budgetSet.setDrawValues(false);

        BarDataSet spentSet = new BarDataSet(spentEntries, "Spent");
        spentSet.setColor(Color.parseColor("#10b981")); 
        spentSet.setDrawValues(false);

        BarData data = new BarData(budgetSet, spentSet);
        data.setBarWidth(0.3f);

        int textColor = ContextCompat.getColor(requireContext(), R.color.text_on_card_secondary);

        binding.barChart.setData(data);
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getLegend().setTextColor(textColor);

        XAxis xAxis = binding.barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);
        xAxis.setDrawGridLines(false);
        xAxis.setCenterAxisLabels(true);

        YAxis leftAxis = binding.barChart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#334155"));

        binding.barChart.getAxisRight().setEnabled(false);
        binding.barChart.groupBars(0f, 0.2f, 0.1f);
        binding.barChart.animateY(500);
        binding.barChart.invalidate();
    }

    private void buildPieChart(List<Business> businesses) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int[] CHART_COLORS = {
                Color.parseColor("#10b981"), Color.parseColor("#3b82f6"),
                Color.parseColor("#f59e0b"), Color.parseColor("#ec4899")
        };

        int colorIdx = 0;
        for (Business b : businesses) {
            if (b.isArchived()) continue;
            if (b.getCurrentSpent() > 0) {
                entries.add(new PieEntry((float) b.getCurrentSpent(), b.getName()));
                colors.add(CHART_COLORS[colorIdx % CHART_COLORS.length]);
                colorIdx++;
            }
        }

        if (entries.isEmpty()) { binding.pieChart.setVisibility(View.GONE); return; }
        if (binding.toggleChartType.getCheckedButtonId() == R.id.btnPieChart) {
            binding.pieChart.setVisibility(View.VISIBLE);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        binding.pieChart.setData(new PieData(dataSet));
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setHoleRadius(40f);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.getLegend().setTextColor(ContextCompat.getColor(requireContext(), R.color.text_on_card_primary));
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.animateY(500);
        binding.pieChart.invalidate();
    }

    private void showAddBusinessDialog() {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_business, null);
        EditText etName = view.findViewById(R.id.etName);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etBudget = view.findViewById(R.id.etBudget);
        MaterialButtonToggleGroup toggleStatus = view.findViewById(R.id.toggleStatus);
        android.widget.RadioGroup rgColors = view.findViewById(R.id.rgColors);

        toggleStatus.check(R.id.btnStatusNow);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add New Business")
                .setView(view)
                .setPositiveButton("Add", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();
                    String budgetStr = etBudget.getText().toString().trim();
                    int checkedId = toggleStatus.getCheckedButtonId();
                    
                    if (name.isEmpty() || budgetStr.isEmpty() || checkedId == View.NO_ID) {
                        Toast.makeText(requireContext(), "Name and Budget are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String status = "now";
                    if (checkedId == R.id.btnStatusIncoming) status = "incoming";
                    else if (checkedId == R.id.btnStatusDone) status = "done";

                    double budget = Double.parseDouble(budgetStr);
                    String selectedColor = "#10b981";
                    int checkedColorId = rgColors.getCheckedRadioButtonId();
                    if (checkedColorId == R.id.rbPurple) selectedColor = "#a855f7";
                    else if (checkedColorId == R.id.rbBlue) selectedColor = "#3b82f6";
                    else if (checkedColorId == R.id.rbAmber) selectedColor = "#f59e0b";
                    else if (checkedColorId == R.id.rbRose) selectedColor = "#f43f5e";

                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
                    
                    Business b = new Business(String.valueOf(System.currentTimeMillis()),
                            name, description, budget, 0, selectedColor, 
                            status, today);
                    
                    storage.addBusiness(b);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (storage != null) storage.removeOnDataChangedListener(this);
        binding = null;
        executor.shutdown();
    }
}
