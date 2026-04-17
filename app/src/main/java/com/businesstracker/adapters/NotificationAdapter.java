package com.businesstracker.adapters;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.businesstracker.R;
import com.businesstracker.models.AppNotification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationActionListener {
        void onDelete(AppNotification notification);
        void onSelectionModeChanged(boolean isSelectionMode);
        void onSelectionChanged(int count);
    }

    private List<AppNotification> notifications;
    private final OnNotificationActionListener listener;
    
    private boolean isSelectionMode = false;
    private final Set<String> selectedIds = new HashSet<>();

    public NotificationAdapter(List<AppNotification> notifications, OnNotificationActionListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    public void updateNotifications(List<AppNotification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notification = notifications.get(position);
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(notification.getCreatedAt());

        if (notification.isRead()) {
            holder.vUnread.setVisibility(View.GONE);
        } else {
            holder.vUnread.setVisibility(View.VISIBLE);
        }

        // Selection background
        if (selectedIds.contains(notification.getId())) {
            holder.itemView.setBackgroundColor(Color.parseColor("#E5E7EB")); // Light gray selection
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        // Setup Icon with circular background
        holder.tvInitial.setVisibility(View.GONE);
        holder.ivIcon.setVisibility(View.VISIBLE);
        holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        holder.ivIcon.setPadding(10, 10, 10, 10);
        holder.ivIcon.setBackgroundResource(R.drawable.bg_avatar_circle);
        holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.white)));

        if ("budget".equals(notification.getType())) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_500)));
        } else if ("task".equals(notification.getType())) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_today);
            holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.stat_tasks)));
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_info);
            holder.ivIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.gray_700)));
        }

        // Hide individual delete button in selection mode
        holder.btnDelete.setVisibility(isSelectionMode ? View.GONE : View.VISIBLE);

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(notification);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectionMode) {
                isSelectionMode = true;
                selectedIds.add(notification.getId());
                notifyDataSetChanged();
                if (listener != null) {
                    listener.onSelectionModeChanged(true);
                    listener.onSelectionChanged(selectedIds.size());
                }
                return true;
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(notification.getId());
            }
        });
    }

    private void toggleSelection(String id) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        
        if (selectedIds.isEmpty()) {
            isSelectionMode = false;
            if (listener != null) listener.onSelectionModeChanged(false);
        }
        
        notifyDataSetChanged();
        if (listener != null) listener.onSelectionChanged(selectedIds.size());
    }

    public void selectAll() {
        selectedIds.clear();
        for (AppNotification n : notifications) {
            selectedIds.add(n.getId());
        }
        isSelectionMode = true;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionModeChanged(true);
            listener.onSelectionChanged(selectedIds.size());
        }
    }

    public void clearSelection() {
        selectedIds.clear();
        isSelectionMode = false;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionModeChanged(false);
            listener.onSelectionChanged(0);
        }
    }

    public Set<String> getSelectedIds() {
        return selectedIds;
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvMessage, tvTime, tvInitial;
        View vUnread;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivNotifIcon);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            tvInitial = itemView.findViewById(R.id.tvNotifInitial);
            vUnread = itemView.findViewById(R.id.vUnreadIndicator);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotif);
        }
    }
}
