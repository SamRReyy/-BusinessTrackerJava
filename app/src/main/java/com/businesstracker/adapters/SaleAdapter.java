package com.businesstracker.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.businesstracker.R;
import com.businesstracker.models.Sale;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class SaleAdapter extends RecyclerView.Adapter<SaleAdapter.ViewHolder> {

    public interface OnSaleActionListener {
        void onDelete(String id);
    }

    private final List<Sale> sales;
    private final OnSaleActionListener listener;

    public SaleAdapter(List<Sale> sales, OnSaleActionListener listener) {
        this.sales = sales;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sale, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sale s = sales.get(position);
        holder.tvDate.setText(s.getDate());
        holder.tvProductName.setText(s.getProductName());
        
        String qty = s.getQuantity() != null ? s.getQuantity() : "0";
        String price = s.getUnitPrice() != null ? s.getUnitPrice() : "0";
        holder.tvQtyPrice.setText(String.format(Locale.US, "%s x ₱%s", qty, price));
        
        holder.tvSaleTotal.setText(String.format(Locale.US, "₱%,.2f", s.getTotalAmount()));

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(s.getId());
        });
    }

    @Override
    public int getItemCount() { return sales.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView       tvDate, tvProductName, tvQtyPrice, tvSaleTotal;
        MaterialButton btnDelete;

        ViewHolder(View v) {
            super(v);
            tvDate = v.findViewById(R.id.tvSaleDate);
            tvProductName = v.findViewById(R.id.tvProductName);
            tvQtyPrice = v.findViewById(R.id.tvQtyPrice);
            tvSaleTotal = v.findViewById(R.id.tvSaleTotal);
            btnDelete = v.findViewById(R.id.btnDeleteSale);
        }
    }
}
