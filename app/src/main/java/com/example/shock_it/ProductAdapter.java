package com.example.shock_it;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout; // 🌟 נדרש לייבוא עבור LinearLayout
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import classes.Item; // הנחה שהקלאס Item נמצא כאן

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private List<Map.Entry<Item, Double>> productList;
    private OnProductActionListener listener;
    private boolean isOwner;

    // 🌟 משתנה חדש: עוקב אחר האינדקס של הפריט הפתוח (אם אין פתוח, הערך הוא -1)
    private int expandedPosition = -1;

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
        this.expandedPosition = -1; // איפוס מצב הפתיחה בטעינת נתונים חדשים
        notifyDataSetChanged();
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

        // 1. הגדרת כותרת המוצר
        holder.productNamePriceTextView.setText("• " + item.getName() + " - " + String.format("%.2f", price) + " ₪");

        // 2. הגדרת התיאור הנסתר
        holder.productDescriptionTextView.setText(item.getDescription());

        // =======================================
        // 🌟 לוגיקת אקורדיון (פתיחה וסגירה) 🌟
        // =======================================

        final boolean isExpanded = position == expandedPosition;

        // הגדרת נראות החלק המוסתר
        holder.expandableLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // טיפול בלחיצה על הכותרת (headerLayout)
        holder.headerLayout.setOnClickListener(v -> {
            int previousExpandedPosition = expandedPosition;
            int adapterPosition = holder.getAdapterPosition(); // מיקום נוכחי

            if (isExpanded) {
                // אם פתוח, סגור אותו
                expandedPosition = -1;
            } else {
                // אם סגור, פתח אותו
                expandedPosition = adapterPosition;
            }

            // עדכון הפריט הנוכחי
            notifyItemChanged(adapterPosition);

            // אם היה פריט קודם פתוח, עדכן אותו כדי לסגור
            if (previousExpandedPosition != -1 && previousExpandedPosition != expandedPosition) {
                notifyItemChanged(previousExpandedPosition);
            }
        });


        // =======================================
        // לוגיקת עריכה ומחיקה (נשארת ב-headerLayout)
        // =======================================

        if (isOwner) {
            holder.editButton.setVisibility(View.VISIBLE);
            holder.deleteButton.setVisibility(View.VISIBLE);

            // ודא שהלחיצות על כפתורי העריכה/מחיקה אינן גורמות לפתיחת האקורדיון!
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

            // אם לא הבעלים, בטל את ה-Onclick על הכפתורים לוודא שאינם מגיבים
            holder.editButton.setOnClickListener(null);
            holder.deleteButton.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {

        // Views קיימים
        TextView productNamePriceTextView;
        ImageButton editButton;
        ImageButton deleteButton;

        // 🌟 Views חדשים לטיפול באקורדיון (עפ"י ה-XML המעודכן)
        LinearLayout headerLayout;
        LinearLayout expandableLayout;
        TextView productDescriptionTextView;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            // קישור Views קיימים
            productNamePriceTextView = itemView.findViewById(R.id.productItemNamePrice);
            editButton = itemView.findViewById(R.id.editProductButton);
            deleteButton = itemView.findViewById(R.id.deleteProductButton);

            // 🌟 קישור Views חדשים - עכשיו הקומפיילר יידע היכן לחפש אותם
            headerLayout = itemView.findViewById(R.id.headerLayout);
            expandableLayout = itemView.findViewById(R.id.expandableLayout);
            productDescriptionTextView = itemView.findViewById(R.id.productDescriptionTextView);
        }
    }
}