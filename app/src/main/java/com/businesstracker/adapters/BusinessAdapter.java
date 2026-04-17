package com.businesstracker.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.businesstracker.R;
import com.businesstracker.models.Business;

import java.util.List;
import java.util.Locale;

public class BusinessAdapter extends RecyclerView.Adapter<BusinessAdapter.ViewHolder> {

    public interface OnBusinessClickListener {
        void onClick(Business business);
        void onLongClick(Business business);
    }

    private final List<Business> businesses;
    private final OnBusinessClickListener listener;

    public BusinessAdapter(List<Business> businesses, OnBusinessClickListener listener) {
        this.businesses = businesses;
        this.listener   = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_business, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Business b = businesses.get(position);
        holder.bind(b, listener);
    }

    @Override
    public int getItemCount() { return businesses.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView  tvName, tvDescription, tvBudgetInfo, tvRemaining, tvProgress, tvBadge, tvProfitAmount, tvProfitBadge;
        View      colorDot;
        ProgressBar progressBar;

        ViewHolder(View itemView) {
            super(itemView);
            tvName        = itemView.findViewById(R.id.tvBusinessName);
            tvDescription = itemView.findViewById(R.id.tvBusinessDescription);
            tvBudgetInfo  = itemView.findViewById(R.id.tvBudgetInfo);
            tvRemaining   = itemView.findViewById(R.id.tvRemaining);
            tvProgress    = itemView.findViewById(R.id.tvProgressText);
            tvBadge       = itemView.findViewById(R.id.tvBadge);
            tvProfitAmount = itemView.findViewById(R.id.tvProfitAmount);
            tvProfitBadge = itemView.findViewById(R.id.tvProfitBadge);
            colorDot      = itemView.findViewById(R.id.colorDot);
            progressBar   = itemView.findViewById(R.id.progressBar);
        }

        void bind(Business b, OnBusinessClickListener listener) {
            tvName.setText(b.getName());
            tvDescription.setText(b.getDescription());
            tvDescription.setVisibility(b.getDescription().isEmpty() ? View.GONE : View.VISIBLE);

            tvBudgetInfo.setText(String.format(Locale.US,
                    "₱%,.0f / ₱%,.0f", b.getCurrentSpent(), b.getTargetBudget()));
            tvRemaining.setText(String.format(Locale.US, "₱%,.0f", b.getRemaining()));
            tvProgress.setText(String.format(Locale.US, "%.1f%% used", b.getProgressPercent()));
            progressBar.setProgress((int) Math.min(b.getProgressPercent(), 100));

            // Profit Calculation
            double profit = b.getProfit();
            tvProfitAmount.setText(String.format(Locale.US, "₱%,.2f", profit));
            
            if (b.getTotalRevenue() == 0 && b.getCurrentSpent() == 0) {
                tvProfitBadge.setText("NO RECORDS");
                tvProfitBadge.setBackgroundColor(Color.GRAY);
                tvProfitAmount.setTextColor(Color.GRAY);
            } else if (profit >= 0) {
                tvProfitAmount.setTextColor(Color.parseColor("#10b981")); // Green for profit
                tvProfitBadge.setText("PROFITABLE");
                tvProfitBadge.setBackgroundColor(Color.parseColor("#064E3B"));
            } else {
                tvProfitAmount.setTextColor(Color.parseColor("#EF4444")); // Red for loss
                tvProfitBadge.setText("LOSS");
                tvProfitBadge.setBackgroundColor(Color.parseColor("#7F1D1D"));
            }

            try {
                colorDot.setBackgroundColor(Color.parseColor(b.getColor()));
            } catch (Exception e) {
                colorDot.setBackgroundColor(Color.parseColor("#10b981"));
            }

            if (b.isOverBudget()) {
                tvBadge.setText("Over Budget");
                tvBadge.setBackgroundColor(Color.parseColor("#FFEF4444"));
                tvBadge.setVisibility(View.VISIBLE);
            } else if (b.isNearBudget()) {
                tvBadge.setText("Near Limit");
                tvBadge.setBackgroundColor(Color.parseColor("#FFF97316"));
                tvBadge.setVisibility(View.VISIBLE);
            } else {
                tvBadge.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onClick(b));
            itemView.setOnLongClickListener(v -> {
                listener.onLongClick(b);
                return true;
            });
        }
    }
}
