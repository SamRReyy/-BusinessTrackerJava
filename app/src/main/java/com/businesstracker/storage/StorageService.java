package com.businesstracker.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.businesstracker.models.AppNotification;
import com.businesstracker.models.Business;
import com.businesstracker.models.Expense;
import com.businesstracker.models.Sale;
import com.businesstracker.models.Task;
import com.businesstracker.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class StorageService {

    private static final String TAG = "StorageService";

    public interface OnDataChangedListener {
        void onNotificationChanged();
        default void onDataSynced() {}
        default void onSettingsChanged() {}
    }

    private static final String PREFS_NAME    = "business_tracker_prefs";
    private static final String KEY_USER      = "user";
    private static final String KEY_BUSINESSES = "businesses";
    private static final String KEY_EXPENSES  = "expenses";
    private static final String KEY_TASKS     = "tasks";
    private static final String KEY_SALES     = "sales";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_CLOUD_SYNC = "cloud_sync";
    private static final String KEY_CURRENCY = "currency_symbol";
    private static final String KEY_HIDE_VALUES = "hide_values_summary";
    private static final String KEY_EMAIL_NOTIF = "email_notif_enabled";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final List<OnDataChangedListener> listeners = new CopyOnWriteArrayList<>();

    private static StorageService instance;

    public static StorageService getInstance(Context context) {
        if (instance == null) {
            instance = new StorageService(context.getApplicationContext());
        }
        return instance;
    }

    private StorageService(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addOnDataChangedListener(OnDataChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeOnDataChangedListener(OnDataChangedListener listener) {
        listeners.remove(listener);
    }

    public void setOnDataChangedListener(OnDataChangedListener listener) {
        if (listener == null) {
            listeners.clear(); 
        } else {
            addOnDataChangedListener(listener);
        }
    }

    private void notifyChange() {
        for (OnDataChangedListener listener : listeners) {
            listener.onNotificationChanged();
        }
    }
    
    private void notifySettingsChange() {
        for (OnDataChangedListener listener : listeners) {
            listener.onSettingsChanged();
        }
    }

    private String getUserId() {
        return FirebaseAuth.getInstance().getUid();
    }

    private String nowIso() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(new Date());
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    public void syncDataFromFirestore() {
        if (!isCloudSyncEnabled()) return;
        
        String uid = getUserId();
        if (uid == null) return;

        Log.d(TAG, "Starting sync for user: " + uid);

        // Sync Businesses
        db.collection("users").document(uid).collection("businesses").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Business> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        list.add(doc.toObject(Business.class));
                    }
                    setBusinesses(list);
                });

        // Sync Expenses
        db.collection("users").document(uid).collection("expenses").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Expense> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        list.add(doc.toObject(Expense.class));
                    }
                    setExpenses(list);
                });

        // Sync Tasks
        db.collection("users").document(uid).collection("tasks").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Task> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        list.add(doc.toObject(Task.class));
                    }
                    setTasks(list);
                });

        // Sync Sales
        db.collection("users").document(uid).collection("sales").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Sale> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        list.add(doc.toObject(Sale.class));
                    }
                    setSales(list);
                    for (OnDataChangedListener l : listeners) l.onDataSynced();
                });
    }

    // ── User ─────────────────────────────────────────────────────────────────

    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        if (json == null) return null;
        return gson.fromJson(json, User.class);
    }

    public void setUser(User user) {
        if (user == null) {
            prefs.edit().remove(KEY_USER).apply();
        } else {
            prefs.edit().putString(KEY_USER, gson.toJson(user)).apply();
            String uid = getUserId();
            if (uid != null && isCloudSyncEnabled()) {
                db.collection("users").document(uid).set(user);
            }
        }
        notifyChange();
    }

    // ── Businesses ────────────────────────────────────────────────────────────

    public List<Business> getBusinesses() {
        String json = prefs.getString(KEY_BUSINESSES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Business>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void setBusinesses(List<Business> list) {
        prefs.edit().putString(KEY_BUSINESSES, gson.toJson(list)).apply();
        notifyChange();
    }

    public void addBusiness(Business business) {
        List<Business> list = getBusinesses();
        list.add(business);
        setBusinesses(list);

        addNotification(new AppNotification(UUID.randomUUID().toString(), "info", 
                "New Business Created", "Business \"" + business.getName() + "\" has been added.", false, nowIso()));

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("businesses").document(business.getId()).set(business);
        }
    }

    public void updateBusiness(String id, Business updated) {
        List<Business> list = getBusinesses();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.set(i, updated);
                break;
            }
        }
        setBusinesses(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("businesses").document(id).set(updated);
        }
    }

    public void deleteBusiness(String id) {
        List<Business> list = getBusinesses();
        list.removeIf(b -> b.getId().equals(id));
        setBusinesses(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("businesses").document(id).delete();
        }
    }

    public void archiveBusiness(String id, boolean archive) {
        List<Business> list = getBusinesses();
        for (Business b : list) {
            if (b.getId().equals(id)) {
                b.setArchived(archive);
                updateBusiness(id, b);
                break;
            }
        }
    }

    // ── Expenses ─────────────────────────────────────────────────────────────

    public List<Expense> getExpenses() {
        String json = prefs.getString(KEY_EXPENSES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Expense>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void setExpenses(List<Expense> list) {
        prefs.edit().putString(KEY_EXPENSES, gson.toJson(list)).apply();
        notifyChange();
    }

    public void addExpense(Expense expense) {
        List<Expense> list = getExpenses();
        list.add(expense);
        setExpenses(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("expenses").document(expense.getId()).set(expense);
        }
        updateBusinessStats(expense.getBusinessId());
    }

    public void updateExpense(String id, Expense updated) {
        List<Expense> list = getExpenses();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.set(i, updated);
                break;
            }
        }
        setExpenses(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("expenses").document(id).set(updated);
        }
        updateBusinessStats(updated.getBusinessId());
    }

    public void deleteExpense(String id) {
        List<Expense> list = getExpenses();
        String businessId = null;
        for (Expense e : list) {
            if (e.getId().equals(id)) {
                businessId = e.getBusinessId();
                break;
            }
        }
        list.removeIf(e -> e.getId().equals(id));
        setExpenses(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("expenses").document(id).delete();
        }
        if (businessId != null) updateBusinessStats(businessId);
    }

    public void updateBusinessStats(String businessId) {
        List<Expense> allExpenses = getExpenses();
        double totalSpent = 0;
        for (Expense e : allExpenses) {
            if (e.getBusinessId().equals(businessId)) {
                totalSpent += e.getAmount();
            }
        }

        List<Sale> allSales = getSales();
        double totalRevenue = 0;
        for (Sale s : allSales) {
            if (s.getBusinessId().equals(businessId)) {
                totalRevenue += s.getTotalAmount();
            }
        }

        List<Business> businesses = getBusinesses();
        for (Business b : businesses) {
            if (b.getId().equals(businessId)) {
                b.setCurrentSpent(totalSpent);
                b.setTotalRevenue(totalRevenue);
                
                if (totalRevenue < totalSpent && totalSpent > 0) {
                    addNotification(new AppNotification(UUID.randomUUID().toString(), "loss",
                            "Loss Alert: " + b.getName(), 
                            "Currently spending more than revenue (Spent: " + totalSpent + ", Revenue: " + totalRevenue + ")", 
                            false, nowIso()));
                }
                
                if (totalSpent > b.getTargetBudget()) {
                    addNotification(new AppNotification(UUID.randomUUID().toString(), "budget",
                            "Over Budget: " + b.getName(), 
                            "You have exceeded your target budget!", false, nowIso()));
                }

                updateBusiness(businessId, b);
                break;
            }
        }
    }

    // ── Tasks ────────────────────────────────────────────────────────────────

    public List<Task> getTasks() {
        String json = prefs.getString(KEY_TASKS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Task>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void setTasks(List<Task> list) {
        prefs.edit().putString(KEY_TASKS, gson.toJson(list)).apply();
        notifyChange();
    }

    public void addTask(Task task) {
        List<Task> list = getTasks();
        list.add(task);
        setTasks(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("tasks").document(task.getId()).set(task);
        }
    }

    public void updateTask(String id, Task updated) {
        List<Task> list = getTasks();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {
                list.set(i, updated);
                break;
            }
        }
        setTasks(list);

        if (updated.isCompleted()) {
             addNotification(new AppNotification(UUID.randomUUID().toString(), "task",
                            "Task Completed", "\"" + updated.getTitle() + "\" is finished!", false, nowIso()));
        }

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("tasks").document(id).set(updated);
        }
    }

    public void deleteTask(String id) {
        List<Task> list = getTasks();
        list.removeIf(t -> t.getId().equals(id));
        setTasks(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("tasks").document(id).delete();
        }
    }

    // ── Sales ────────────────────────────────────────────────────────────────

    public List<Sale> getSales() {
        String json = prefs.getString(KEY_SALES, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Sale>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private void setSales(List<Sale> list) {
        prefs.edit().putString(KEY_SALES, gson.toJson(list)).apply();
        notifyChange();
    }

    public void addSale(Sale sale) {
        List<Sale> list = getSales();
        list.add(sale);
        setSales(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("sales").document(sale.getId()).set(sale);
        }
        updateBusinessStats(sale.getBusinessId());
    }

    public void deleteSale(String id) {
        List<Sale> list = getSales();
        String businessId = null;
        for (Sale s : list) {
            if (s.getId().equals(id)) {
                businessId = s.getBusinessId();
                break;
            }
        }
        list.removeIf(s -> s.getId().equals(id));
        setSales(list);

        String uid = getUserId();
        if (uid != null && isCloudSyncEnabled()) {
            db.collection("users").document(uid).collection("sales").document(id).delete();
        }
        if (businessId != null) updateBusinessStats(businessId);
    }

    public void clearAllData() {
        prefs.edit()
                .remove(KEY_USER)
                .remove(KEY_BUSINESSES)
                .remove(KEY_EXPENSES)
                .remove(KEY_TASKS)
                .remove(KEY_SALES)
                .remove(KEY_NOTIFICATIONS)
                .apply();
        notifyChange();
    }

    public boolean isDarkMode() { return prefs.getBoolean(KEY_DARK_MODE, false); }
    public void setDarkMode(boolean enabled) { 
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply(); 
        addNotification(new AppNotification(UUID.randomUUID().toString(), "info", 
                "Theme Changed", "App appearance updated.", false, nowIso()));
    }

    public boolean isCloudSyncEnabled() { return prefs.getBoolean(KEY_CLOUD_SYNC, true); }
    public void setCloudSyncEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_CLOUD_SYNC, enabled).apply(); }

    public String getCurrency() { return prefs.getString(KEY_CURRENCY, "₱"); }
    public void setCurrency(String currency) { 
        prefs.edit().putString(KEY_CURRENCY, currency).apply(); 
        notifySettingsChange();
    }

    public boolean isHideValuesEnabled() { return prefs.getBoolean(KEY_HIDE_VALUES, false); }
    public void setHideValuesEnabled(boolean enabled) { 
        prefs.edit().putBoolean(KEY_HIDE_VALUES, enabled).apply(); 
        notifySettingsChange();
    }

    public boolean isEmailNotifEnabled() { return prefs.getBoolean(KEY_EMAIL_NOTIF, true); }
    public void setEmailNotifEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_EMAIL_NOTIF, enabled).apply(); }

    public void addNotification(AppNotification n) { 
        List<AppNotification> list = getNotifications();
        list.add(0, n);
        if (list.size() > 50) list = list.subList(0, 50);
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(list)).apply();
        notifyChange();
    }

    public List<AppNotification> getNotifications() { 
        String json = prefs.getString(KEY_NOTIFICATIONS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<AppNotification>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public void markAllNotificationsRead() {
        List<AppNotification> list = getNotifications();
        for (AppNotification n : list) {
            n.setRead(true);
        }
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(list)).apply();
        notifyChange();
    }

    public int getUnreadNotificationCount() {
        List<AppNotification> list = getNotifications();
        int count = 0;
        for (AppNotification n : list) {
            if (!n.isRead()) count++;
        }
        return count;
    }

    public void deleteNotification(String id) {
        List<AppNotification> list = getNotifications();
        list.removeIf(n -> n.getId().equals(id));
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(list)).apply();
        notifyChange();
    }

    public void deleteNotifications(Set<String> ids) {
        List<AppNotification> list = getNotifications();
        list.removeIf(n -> ids.contains(n.getId()));
        prefs.edit().putString(KEY_NOTIFICATIONS, gson.toJson(list)).apply();
        notifyChange();
    }
}
