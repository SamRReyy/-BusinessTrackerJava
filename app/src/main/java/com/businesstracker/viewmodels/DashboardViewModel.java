package com.businesstracker.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.businesstracker.models.Business;
import com.businesstracker.models.Expense;
import com.businesstracker.models.Sale;
import com.businesstracker.storage.StorageService;

import java.util.ArrayList;
import java.util.List;

public class DashboardViewModel extends AndroidViewModel implements StorageService.OnDataChangedListener {

    private final StorageService storage;
    private final MutableLiveData<List<Business>> businesses = new MutableLiveData<>();
    private final MutableLiveData<List<Expense>> expenses = new MutableLiveData<>();
    private final MutableLiveData<List<Sale>> sales = new MutableLiveData<>();
    private final MutableLiveData<Double> totalBudget = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalSpent = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> totalRevenue = new MutableLiveData<>(0.0);

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        storage = StorageService.getInstance(application);
        storage.setOnDataChangedListener(this);
        loadData();
    }

    public LiveData<List<Business>> getBusinesses() { return businesses; }
    public LiveData<List<Expense>> getExpenses() { return expenses; }
    public LiveData<List<Sale>> getSales() { return sales; }
    public LiveData<Double> getTotalBudget() { return totalBudget; }
    public LiveData<Double> getTotalSpent() { return totalSpent; }
    public LiveData<Double> getTotalRevenue() { return totalRevenue; }

    public void loadData() {
        List<Business> businessList = storage.getBusinesses();
        List<Expense> expenseList = storage.getExpenses();
        List<Sale> saleList = storage.getSales();

        businesses.setValue(businessList);
        expenses.setValue(expenseList);
        sales.setValue(saleList);

        double budget = 0, spent = 0, revenue = 0;
        for (Business b : businessList) {
            budget += b.getTargetBudget();
            spent += b.getCurrentSpent();
            revenue += b.getTotalRevenue();
        }
        totalBudget.setValue(budget);
        totalSpent.setValue(spent);
        totalRevenue.setValue(revenue);
    }

    @Override
    public void onNotificationChanged() {
        loadData();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        storage.setOnDataChangedListener(null);
    }
}
