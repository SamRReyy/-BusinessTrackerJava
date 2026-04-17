package com.businesstracker.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.businesstracker.R;
import com.businesstracker.models.Expense;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public interface OnExpenseActionListener {
        void onDelete(String id);
        void onEdit(Expense expense);
        void onPhotoClick(Bitmap photo);
    }

    private final List<Expense> expenses;
    private final OnExpenseActionListener listener;

    public ExpenseAdapter(List<Expense> expenses, OnExpenseActionListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Expense e = expenses.get(position);
        holder.tvType.setText(e.getType());
        holder.tvAmount.setText(String.format(Locale.US, "₱%,.0f", e.getAmount()));
        holder.tvDate.setText(e.getDate());
        holder.tvDescription.setText(e.getDescription());
        
        holder.tvDescription.setVisibility(
                (e.getDescription() == null || e.getDescription().isEmpty()) ? View.GONE : View.VISIBLE);

        if (e.getPhoto() != null && !e.getPhoto().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(e.getPhoto(), Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.ivThumbnail.setImageBitmap(decodedByte);
                holder.ivThumbnail.setVisibility(View.VISIBLE);

                holder.ivThumbnail.setOnClickListener(v -> {
                    if (listener != null) listener.onPhotoClick(decodedByte);
                });
            } catch (Exception ex) {
                holder.ivThumbnail.setVisibility(View.GONE);
            }
        } else {
            holder.ivThumbnail.setVisibility(View.GONE);
            holder.ivThumbnail.setOnClickListener(null);
        }

        holder.btnDelete.setOnClickListener(v -> listener.onDelete(e.getId()));
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(e));
    }

    @Override
    public int getItemCount() { return expenses.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView       tvType, tvAmount, tvDate, tvDescription;
        ImageView      ivThumbnail;
        MaterialButton btnDelete, btnEdit;

        ViewHolder(View v) {
            super(v);
            tvType        = v.findViewById(R.id.tvExpenseType);
            tvAmount      = v.findViewById(R.id.tvExpenseAmount);
            tvDate        = v.findViewById(R.id.tvExpenseDate);
            tvDescription = v.findViewById(R.id.tvExpenseDescription);
            ivThumbnail   = v.findViewById(R.id.ivExpenseThumbnail);
            btnDelete     = v.findViewById(R.id.btnDeleteExpense);
            btnEdit       = v.findViewById(R.id.btnEditExpense);
        }
    }
}
