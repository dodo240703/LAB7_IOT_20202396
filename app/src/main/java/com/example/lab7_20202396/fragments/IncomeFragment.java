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
import com.example.lab7_20202396.databinding.FragmentIncomeBinding;
import com.example.lab7_20202396.model.Income;
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

public class IncomeFragment extends Fragment {

    private FragmentIncomeBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<Income> incomeList;
    private IncomeAdapter adapter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final Calendar calendar = Calendar.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIncomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        incomeList = new ArrayList<>();

        setupRecyclerView();

        binding.fabAddIncome.setOnClickListener(v -> showAddIncomeDialog(null));

        loadIncomes();
    }

    private void setupRecyclerView() {
        adapter = new IncomeAdapter(incomeList,
                income -> showAddIncomeDialog(income),  // Edit click listener
                income -> showDeleteConfirmationDialog(income)  // Delete click listener
        );
        binding.recyclerViewIncome.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewIncome.setAdapter(adapter);
    }

    private void loadIncomes() {
        showLoading(true);

        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();

            db.collection("incomes")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        incomeList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Income income = document.toObject(Income.class);
                            incomeList.add(income);
                        }

                        if (incomeList.isEmpty()) {
                            binding.textViewEmptyIncome.setVisibility(View.VISIBLE);
                        } else {
                            binding.textViewEmptyIncome.setVisibility(View.GONE);
                        }

                        adapter.notifyDataSetChanged();
                        showLoading(false);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(requireContext(), "Error al cargar ingresos: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    });
        }
    }

    private void showAddIncomeDialog(Income income) {
        boolean isEdit = income != null;

        // Inflar layout personalizado para el diálogo
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_income, null);

        EditText editTextTitle = dialogView.findViewById(R.id.editTextIncomeTitle);
        EditText editTextAmount = dialogView.findViewById(R.id.editTextIncomeAmount);
        EditText editTextDescription = dialogView.findViewById(R.id.editTextIncomeDescription);
        EditText editTextDate = dialogView.findViewById(R.id.editTextIncomeDate);

        // Si estamos editando, llenar campos y deshabilitar título según requisitos
        if (isEdit) {
            editTextTitle.setText(income.getTitle());
            editTextTitle.setEnabled(false); // No se puede editar el título
            editTextAmount.setText(String.valueOf(income.getAmount()));
            editTextDescription.setText(income.getDescription());
            editTextDate.setText(dateFormat.format(income.getDate()));
            editTextDate.setEnabled(false); // No se puede editar la fecha
        } else {
            // Para nuevo ingreso, colocar fecha actual
            editTextDate.setText(dateFormat.format(new Date()));
        }

        // Configurar selector de fecha
        editTextDate.setOnClickListener(v -> {
            if (!isEdit) { // Solo permitimos cambiar fecha si es nuevo ingreso
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
                // Actualizar ingreso existente
                updateIncome(income, amount, description);
            } else {
                // Crear nuevo ingreso
                saveIncome(title, amount, description, date);
            }

            dialog.dismiss();
        });
    }

    private void saveIncome(String title, double amount, String description, Date date) {
        if (auth.getCurrentUser() != null) {
            showLoading(true);

            String userId = auth.getCurrentUser().getUid();
            String id = db.collection("incomes").document().getId();

            // Convertir Date a long (timestamp)
            long timestamp = date.getTime();
            Income income = new Income(id, title, amount, description, timestamp, userId);

            db.collection("incomes")
                    .document(id)
                    .set(income)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), R.string.income_saved, Toast.LENGTH_SHORT).show();
                        loadIncomes();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateIncome(Income income, double newAmount, String newDescription) {
        if (auth.getCurrentUser() != null) {
            showLoading(true);

            db.collection("incomes")
                    .document(income.getId())
                    .update(
                            "amount", newAmount,
                            "description", newDescription
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(requireContext(), R.string.income_updated, Toast.LENGTH_SHORT).show();
                        loadIncomes();
                    })
                    .addOnFailureListener(e -> {
                        showLoading(false);
                        Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showDeleteConfirmationDialog(Income income) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete)
                .setMessage(R.string.confirm_delete)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteIncome(income))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void deleteIncome(Income income) {
        showLoading(true);

        db.collection("incomes")
                .document(income.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), R.string.income_deleted, Toast.LENGTH_SHORT).show();
                    loadIncomes();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        binding.progressBarIncome.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.recyclerViewIncome.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Interfaz para el adaptador
    public interface IncomeListener {
        void onIncomeClick(Income income);
    }

    // Clase adaptador para RecyclerView (para simplificar, la definimos aquí)
    private static class IncomeAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<IncomeAdapter.ViewHolder> {

        private final List<Income> incomes;
        private final IncomeListener editListener;
        private final IncomeListener deleteListener;

        public IncomeAdapter(List<Income> incomes, IncomeListener editListener, IncomeListener deleteListener) {
            this.incomes = incomes;
            this.editListener = editListener;
            this.deleteListener = deleteListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_income, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Income income = incomes.get(position);
            holder.bind(income, editListener, deleteListener);
        }

        @Override
        public int getItemCount() {
            return incomes.size();
        }

        // ViewHolder
        static class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final View itemView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                this.itemView = itemView;
            }

            public void bind(Income income, IncomeListener editListener, IncomeListener deleteListener) {
                // Configurar vistas del ítem
                android.widget.TextView titleTextView = itemView.findViewById(R.id.textViewIncomeTitle);
                android.widget.TextView amountTextView = itemView.findViewById(R.id.textViewIncomeAmount);
                android.widget.TextView descriptionTextView = itemView.findViewById(R.id.textViewIncomeDescription);
                android.widget.TextView dateTextView = itemView.findViewById(R.id.textViewIncomeDate);
                android.widget.Button editButton = itemView.findViewById(R.id.buttonEditIncome);
                android.widget.Button deleteButton = itemView.findViewById(R.id.buttonDeleteIncome);

                // Configurar datos
                titleTextView.setText(income.getTitle());
                amountTextView.setText(String.format(Locale.getDefault(), "S/ %.2f", income.getAmount()));
                descriptionTextView.setText(income.getDescription());
                dateTextView.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(income.getDate()));

                // Configurar listeners
                editButton.setOnClickListener(v -> editListener.onIncomeClick(income));
                deleteButton.setOnClickListener(v -> deleteListener.onIncomeClick(income));
            }
        }
    }
}
