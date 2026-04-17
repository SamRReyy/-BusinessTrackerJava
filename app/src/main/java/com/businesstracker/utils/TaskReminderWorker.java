package com.businesstracker.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.businesstracker.R;
import com.businesstracker.models.Task;
import com.businesstracker.storage.StorageService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskReminderWorker extends Worker {

    public TaskReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        StorageService storage = StorageService.getInstance(getApplicationContext());
        List<Task> tasks = storage.getTasks();
        long now = System.currentTimeMillis();
        long oneDayMillis = 24 * 60 * 60 * 1000;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        for (Task task : tasks) {
            if (!task.isCompleted() && !task.isNotified()) {
                try {
                    Date dueDate = sdf.parse(task.getDueDate());
                    if (dueDate != null) {
                        long diff = dueDate.getTime() - now;
                        // Notify if task is due within the next 24 hours
                        if (diff > 0 && diff <= oneDayMillis) {
                            sendNotification(task);
                            task.setNotified(true);
                            storage.updateTask(task.getId(), task);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return Result.success();
    }

    private void sendNotification(Task task) {
        NotificationManager notificationManager = (NotificationManager) 
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "task_reminders";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Task Reminder")
                .setContentText("Your task \"" + task.getTitle() + "\" is due soon!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        notificationManager.notify(task.getId().hashCode(), builder.build());
    }
}
