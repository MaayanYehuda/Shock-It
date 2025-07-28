// Assuming this is in com.example.shock_it.adapters
package com.example.shock_it;

import android.content.Context; // Not strictly needed in adapter, but okay
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shock_it.R;
import classes.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Map.Entry<Item, Double>> productList;
    private OnProductActionListener listener;
    private boolean isOwner; // To show/hide edit/delete buttons

    public interface OnProductActionListener {
        void onEditProduct(Item item, double price);
        void onDeleteProduct(Item item);

    }

    public ProductAdapter(OnProductActionListener listener) {
        this.productList = new ArrayList<>();
        this.listener = listener;
    }

    public void setProducts(Map<Item, Double> productsMap, boolean isOwner) {
        this.productList.clear();
        if (productsMap != null) {
            this.productList.addAll(productsMap.entrySet());
        }
        this.isOwner = isOwner;
        notifyDataSetChanged(); // Important: Notify adapter of data change
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Map.Entry<Item, Double> entry = productList.get(position);
        Item item = entry.getKey();
        Double price = entry.getValue();

        holder.productNamePriceTextView.setText("• " + item.getName() + " - " + String.format("%.2f", price) + " ₪");

        if (isOwner) {
            holder.editButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);

            holder.editButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEditProduct(item, price);
                }
            });

            holder.deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteProduct(item);
                }
            });
        } else {
            holder.editButton.setVisibility(View.GONE);
            holder.deleteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView productNamePriceTextView;
        ImageButton editButton;
        ImageButton deleteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productNamePriceTextView = itemView.findViewById(R.id.productItemNamePrice);
            editButton = itemView.findViewById(R.id.editProductButton);
            deleteButton = itemView.findViewById(R.id.deleteProductButton);
        }
    }
}