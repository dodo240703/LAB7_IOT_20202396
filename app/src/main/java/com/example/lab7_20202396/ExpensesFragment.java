package com.example.lab7_20202396;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lab7_20202396.adapters.ExpenseAdapter;
import com.example.lab7_20202396.databinding.FragmentExpensesBinding;
import com.example.lab7_20202396.model.Expense;
import com.example.lab7_20202396.service.FinanceService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private FragmentExpensesBinding binding;
    private FinanceService financeService;
    private ExpenseAdapter adapter;
    private List<Expense> expensesList;
    private Calendar selectedDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExpensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        financeService = new FinanceService();
        expensesList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        setupRecyclerView();
        setupAddButton();
        updateMonthText();
        loadExpenses();

        // Botones para cambiar de mes
        binding.buttonPreviousMonth.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, -1);
            updateMonthText();
            loadExpenses();
        });

        binding.buttonNextMonth.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, 1);
            updateMonthText();
            loadExpenses();
        });
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(expensesList, this::showEditDeleteDialog);
        binding.recyclerViewExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewExpenses.setAdapter(adapter);
    }

    private void setupAddButton() {
        binding.fabAddExpense.setOnClickListener(v -> showAddExpenseDialog());
    }

    private void updateMonthText() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        binding.textViewMonth.setText(sdf.format(selectedDate.getTime()));
    }

    private void loadExpenses() {
        binding.progressBar.setVisibility(View.VISIBLE);

        financeService.getExpensesByMonth(
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                new FinanceService.DataCallback<List<Expense>>() {
                    @Override
                    public void onSuccess(List<Expense> result) {
                        if (isAdded()) {
                            binding.progressBar.setVisibility(View.GONE);
                            expensesList.clear();
                            expensesList.addAll(result);
                            adapter.notifyDataSetChanged();

                            // Mostrar mensaje si no hay gastos
                            if (expensesList.isEmpty()) {
                                binding.textViewNoExpenses.setVisibility(View.VISIBLE);
                                binding.recyclerViewExpenses.setVisibility(View.GONE);
                            } else {
                                binding.textViewNoExpenses.setVisibility(View.GONE);
                                binding.recyclerViewExpenses.setVisibility(View.VISIBLE);
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (isAdded()) {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showAddExpenseDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);

        // Configurar selector de fecha
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));

        etDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        etDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_expense)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (title.isEmpty() || amountStr.isEmpty()) {
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

                    Expense expense = new Expense(
                            title,
                            amount,
                            description,
                            calendar.getTimeInMillis(),
                            financeService.getCurrentUser().getUid()
                    );

                    saveExpense(expense);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditDeleteDialog(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(expense.getTitle())
                .setItems(new String[]{getString(R.string.edit), getString(R.string.delete)}, (dialog, which) -> {
                    if (which == 0) {
                        showEditExpenseDialog(expense);
                    } else {
                        showDeleteConfirmationDialog(expense);
                    }
                })
                .show();
    }

    private void showEditExpenseDialog(Expense expense) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        TextInputEditText etTitle = dialogView.findViewById(R.id.etTitle);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etAmount);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        TextInputEditText etDate = dialogView.findViewById(R.id.etDate);

        // Cargar datos del gasto
        etTitle.setText(expense.getTitle());
        etAmount.setText(String.valueOf(expense.getAmount()));
        etDescription.setText(expense.getDescription());

        // Configurar fecha
        Calendar calendar = Calendar.getInstance();
        // Usar setTime en lugar de setTimeInMillis ya que ahora tenemos un objeto Date
        calendar.setTime(expense.getDate());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        etDate.setText(sdf.format(calendar.getTime()));

        etDate.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, month);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        etDate.setText(sdf.format(calendar.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_expense)
                .setView(dialogView)
                .setPositiveButton(R.string.update, (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String amountStr = etAmount.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (title.isEmpty() || amountStr.isEmpty()) {
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

                    // Actualizar objeto
                    expense.setTitle(title);
                    expense.setAmount(amount);
                    expense.setDescription(description);
                    expense.setDate(calendar.getTime());

                    updateExpense(expense);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteConfirmationDialog(Expense expense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirm_delete)
                .setMessage("¿Seguro que quieres eliminar '" + expense.getTitle() + "'?")
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteExpense(expense))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void saveExpense(Expense expense) {
        binding.progressBar.setVisibility(View.VISIBLE);

        financeService.saveExpense(expense)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.expense_saved, Toast.LENGTH_SHORT).show();
                    loadExpenses(); // Recargar lista
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExpense(Expense expense) {
        binding.progressBar.setVisibility(View.VISIBLE);

        financeService.updateExpense(expense)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.expense_updated, Toast.LENGTH_SHORT).show();
                    loadExpenses(); // Recargar lista
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteExpense(Expense expense) {
        binding.progressBar.setVisibility(View.VISIBLE);

        financeService.deleteExpense(expense.getId())
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.expense_deleted, Toast.LENGTH_SHORT).show();
                    loadExpenses(); // Recargar lista
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
