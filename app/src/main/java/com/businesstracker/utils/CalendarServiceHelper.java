package com.businesstracker.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CalendarServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Calendar mCalendarService;

    public CalendarServiceHelper(Calendar calendarService) {
        mCalendarService = calendarService;
    }

    /**
     * Creates an event in the user's primary Google Calendar.
     */
    public Task<String> createEvent(String title, String description, String dueDate) {
        return Tasks.call(mExecutor, () -> {
            Event event = new Event()
                    .setSummary("Task: " + title)
                    .setDescription(description);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date;
            try {
                date = sdf.parse(dueDate);
            } catch (Exception e) {
                date = new Date();
            }

            DateTime startDateTime = new DateTime(date);
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime);
            event.setStart(start);

            // Set end time to 1 hour after start
            long oneHourLater = date.getTime() + 3600000;
            DateTime endDateTime = new DateTime(new Date(oneHourLater));
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime);
            event.setEnd(end);

            Event createdEvent = mCalendarService.events().insert("primary", event).execute();
            if (createdEvent == null) {
                throw new IOException("Null result when creating calendar event.");
            }

            return createdEvent.getHtmlLink();
        });
    }
}
