package com.example.lab7_20202396.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog; // Importación correcta para AlertDialog
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lab7_20202396.R;
import com.example.lab7_20202396.databinding.FragmentExpenseBinding;
import com.example.lab7_20202396.model.Expense;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseFragment extends Fragment {

    private FragmentExpenseBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Expense> expenseList;
    private ExpenseAdapter adapter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentExpenseBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        expenseList = new ArrayList<>();

        setupRecyclerView();

        binding.fabAddExpense.setOnClickListener(v -> showAddExpenseDialog(null));

        loadExpenses();
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(expenseList,
                expense -> showAddExpenseDialog(expense),  // Edit click listener
                expense -> showDeleteConfirmationDialog(expense)  // Delete click listener
        );
        binding.recyclerViewExpense.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewExpense.setAdapter(adapter);
    }

    private void loadExpenses() {
        showLoading(true);

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();

            db.collection("expenses")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        expenseList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Expense expense = document.toObject(Expense.class);
                            expenseList.add(expense);
                        }

                        if (expenseList.isEmpty()) {
                            binding.textViewEmptyExpense.setVisibility(View.VISIBLE);
                        } else {
                            binding.textViewEmptyExpense.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                        showLoading(false);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Error al cargar egresos: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
        }
    }

    private void showAddExpenseDialog(Expense expense) {
        boolean isEdit = expense != null;

        // Inflar layout personalizado para el diálogo
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_expense, null);

        EditText editTextTitle = dialogView.findViewById(R.id.editTextExpenseTitle);
        EditText editTextAmount = dialogView.findViewById(R.id.editTextExpenseAmount);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextExpenseDescription);
        EditText editTextDate = dialogView.findViewById(R.id.editTextExpenseDate);

        // Si estamos editando, llenar campos y deshabilitar título según requisitos
        if (isEdit) {
            editTextTitle.setText(expense.getTitle());
            editTextTitle.setEnabled(false); // No se puede editar el título
            editTextAmount.setText(String.valueOf(expense.getAmount()));
            editTextDescription.setText(expense.getDescription());
            editTextDate.setText(dateFormat.format(expense.getDate()));
            editTextDate.setEnabled(false); // No se puede editar la fecha
        } else {
            // Para nuevo egreso, colocar fecha actual
            editTextDate.setText(dateFormat.format(new Date()));
        }

        // Configurar selector de fecha
        editTextDate.setOnClickListener(v -> {
            if (!isEdit) { // Solo permitimos cambiar fecha si es nuevo egreso
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        requireContext(),
                        (view, year, month, dayOfMonth) -> {
                            calendar.set(year, month, dayOfMonth);
                            editTextDate.setText(dateFormat.format(calendar.getTime()));
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            }
        });

        // Construir diálogo
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(isEdit ? R.string.edit : R.string.add)
                .setView(dialogView)
                .setPositiveButton(isEdit ? R.string.update : R.string.save, null)
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Manejar clic en botón positivo (guardar/actualizar)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // Validar entradas
            String title = editTextTitle.getText().toString().trim();
            String amountStr = editTextAmount.getText().toString().trim();
            String description = editTextDescription.getText().toString().trim();
            String dateStr = editTextDate.getText().toString().trim();

            if (title.isEmpty() || amountStr.isEmpty() || dateStr.isEmpty()) {
                Toast.makeText(requireContext(), R.string.required_field, Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            Date date;
            try {
                date = dateFormat.parse(dateStr);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Fecha inválida", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isEdit) {
                // Actualizar egreso existente
                updateExpense(expense, amount, description);
            } else {
                // Crear nuevo egreso
                saveExpense(title, amount, description, date);
            }

            dialog.dismiss();
        });
    }

    private void saveExpense(String title, double amount, String description, Date date) {
        if (auth.getCurrentUser() != null) {
            showLoading(true);

            String userId = auth.getCurrentUser().getUid();
            String id = db.collection("expenses").document().getId();

            Expense expense = new Expense(id, title, amount, description, date, userId);

            db.collection("expenses")
                    .document(id)
                    .set(expense)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), R.string.expense_saved, Toast.LENGTH_SHORT).show();
                        loadExpenses();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateExpense(Expense expense, double newAmount, String newDescription) {
        if (auth.getCurrentUser() != null) {
            showLoading(true);

            db.collection("expenses")
                    .document(expense.getId())
                    .update(
                            "amount", newAmount,
                            "description", newDescription
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), R.string.expense_updated, Toast.LENGTH_SHORT).show();
                        loadExpenses();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDeleteConfirmationDialog(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteExpense(expense))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteExpense(Expense expense) {
        showLoading(true);

        db.collection("expenses")
                .document(expense.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                    loadExpenses();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        binding.progressBarExpense.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerViewExpense.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Interfaz para el adaptador
    public interface ExpenseListener {
        void onExpenseClick(Expense expense);
    }

    // Clase adaptador para RecyclerView
    private static class ExpenseAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

        private final List<Expense> expenses;
        private final ExpenseListener editListener;
        private final ExpenseListener deleteListener;

        public ExpenseAdapter(List<Expense> expenses, ExpenseListener editListener, ExpenseListener deleteListener) {
            this.expenses = expenses;
            this.editListener = editListener;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Expense expense = expenses.get(position);
            holder.bind(expense, editListener, deleteListener);
        }

        @Override
        public int getItemCount() {
            return expenses.size();
        }

        // ViewHolder
        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final View itemView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
            }

            public void bind(Expense expense, ExpenseListener editListener, ExpenseListener deleteListener) {
                // Configurar vistas del ítem
                android.widget.TextView titleTextView = itemView.findViewById(R.id.textViewExpenseTitle);
                android.widget.TextView amountTextView = itemView.findViewById(R.id.textViewExpenseAmount);
                android.widget.TextView descriptionTextView = itemView.findViewById(R.id.textViewExpenseDescription);
                android.widget.TextView dateTextView = itemView.findViewById(R.id.textViewExpenseDate);
                android.widget.Button editButton = itemView.findViewById(R.id.buttonEditExpense);
                android.widget.Button deleteButton = itemView.findViewById(R.id.buttonDeleteExpense);

                // Configurar datos
                titleTextView.setText(expense.getTitle());
                amountTextView.setText(String.format(Locale.getDefault(), "S/ %.2f", expense.getAmount()));
                descriptionTextView.setText(expense.getDescription());
                dateTextView.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.getDate()));

                // Configurar listeners
                editButton.setOnClickListener(v -> editListener.onExpenseClick(expense));
                deleteButton.setOnClickListener(v -> deleteListener.onExpenseClick(expense));
            }
        }
    }
}
