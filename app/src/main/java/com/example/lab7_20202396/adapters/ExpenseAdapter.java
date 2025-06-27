package com.example.lab7_20202396.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab7_20202396.R;
import com.example.lab7_20202396.model.Expense;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenses;
    private final OnExpenseClickListener listener;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public ExpenseAdapter(List<Expense> expenses, OnExpenseClickListener listener) {
        this.expenses = expenses;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(expenses.get(position));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewTitle;
        private final TextView textViewAmount;
        private final TextView textViewDescription;
        private final TextView textViewDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewExpenseTitle);
            textViewAmount = itemView.findViewById(R.id.textViewExpenseAmount);
            textViewDescription = itemView.findViewById(R.id.textViewExpenseDescription);
            textViewDate = itemView.findViewById(R.id.textViewExpenseDate);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onExpenseClick(expenses.get(position));
                }
            });
        }

        public void bind(Expense expense) {
            textViewTitle.setText(expense.getTitle());

            // Formatear cantidad como moneda con signo menos
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            textViewAmount.setText("-" + currencyFormat.format(expense.getAmount()));

            // Mostrar descripción si existe
            if (expense.getDescription() != null && !expense.getDescription().isEmpty()) {
                textViewDescription.setText(expense.getDescription());
                textViewDescription.setVisibility(View.VISIBLE);
            } else {
                textViewDescription.setVisibility(View.GONE);
            }

            // Formatear fecha
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            // No necesitamos crear un nuevo objeto Date, ya tenemos uno
            textViewDate.setText(dateFormat.format(expense.getDate()));
        }
    }
}
