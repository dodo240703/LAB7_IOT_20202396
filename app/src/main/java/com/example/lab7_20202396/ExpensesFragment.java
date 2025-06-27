package com.example.lab7_20202396;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lab7_20202396.adapters.ExpenseAdapter;
import com.example.lab7_20202396.databinding.FragmentExpensesBinding;
import com.example.lab7_20202396.model.Expense;
import com.example.lab7_20202396.service.FinanceService;
import com.example.lab7_20202396.service.ServicioAlmacenamiento;
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
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private ExpenseAdapter adapter;
    private List<Expense> expensesList;
    private Calendar selectedDate;

    // Variables para la selección de imágenes
    private Uri selectedImageUri = null;
    private TextView tvImageStatus;
    private static final String TAG = "ExpensesFragment";

    // Lanzadores para permisos y selección de imágenes
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private View currentDialogView;

    // Constante para la selección de imágenes
    private static final int REQUEST_IMAGE_PICK = 100;

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
        servicioAlmacenamiento = new ServicioAlmacenamiento();
        expensesList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        setupRecyclerView();
        setupAddButton();
        updateMonthText();
        loadExpenses();

        // Inicializar lanzadores
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permiso concedido, abrir selector de imágenes
                        openImagePicker();
                    } else {
                        // Permiso denegado
                        Toast.makeText(requireContext(),
                                "Se requiere permiso para acceder a las imágenes",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        // Inicializar el lanzador para la selección de imágenes
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            Log.d(TAG, "Imagen seleccionada: " + selectedImageUri);

                            // Actualizar UI si el diálogo sigue visible
                            if (currentDialogView != null) {
                                TextView statusText = currentDialogView.findViewById(R.id.tvImageStatus);
                                if (statusText != null) {
                                    statusText.setText("Imagen seleccionada");
                                }
                            }
                        }
                    }
                });

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
        tvImageStatus = dialogView.findViewById(R.id.tvImageStatus);

        currentDialogView = dialogView; // Guardar referencia del diálogo actual

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

        dialogView.findViewById(R.id.btnSelectImage).setOnClickListener(v -> {
            // Verificar y solicitar permiso de lectura de almacenamiento
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
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
                            financeService.getCurrentUser().getUid(),
                            null  // imageUrl - será actualizada después de subir la imagen
                    );

                    saveExpense(expense);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

            // Usar Intent.createChooser para mostrar un selector más amigable
            startActivityForResult(
                    Intent.createChooser(intent, "Seleccionar imagen"),
                    REQUEST_IMAGE_PICK
            );

            // Registrar el intento para depuración
            Log.d(TAG, "Intent para seleccionar imagen lanzado");
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir selector de imágenes: " + e.getMessage(), e);
            Toast.makeText(requireContext(),
                    "Error al abrir el selector de imágenes: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Implementamos el método tradicional como respaldo
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                Log.d(TAG, "Imagen seleccionada (método tradicional): " + selectedImageUri);

                // Actualizar UI si el diálogo sigue visible
                if (currentDialogView != null) {
                    TextView statusText = currentDialogView.findViewById(R.id.tvImageStatus);
                    if (statusText != null) {
                        statusText.setText("Imagen seleccionada");
                    }
                }
            }
        }
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
        tvImageStatus = dialogView.findViewById(R.id.tvImageStatus);

        currentDialogView = dialogView; // Guardar referencia del diálogo actual

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

        dialogView.findViewById(R.id.btnSelectImage).setOnClickListener(v -> {
            // Verificar y solicitar permiso de lectura de almacenamiento
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                openImagePicker();
            }
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

        // Si hay una imagen seleccionada, subirla primero
        if (selectedImageUri != null) {
            // Mostrar mensaje de carga
            Toast.makeText(requireContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show();

            String userId = financeService.getCurrentUser().getUid();
            servicioAlmacenamiento.guardarArchivo(selectedImageUri, "egreso", userId)
                    .addOnSuccessListener(imageUrl -> {
                        // Establecer la URL de la imagen en el gasto
                        expense.setImageUrl(imageUrl.toString());

                        // Guardar el gasto con la URL de la imagen
                        finishSaveExpense(expense);
                    })
                    .addOnFailureListener(e -> {
                        binding.progressBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // No hay imagen, guardar el gasto directamente
            finishSaveExpense(expense);
        }
    }

    private void finishSaveExpense(Expense expense) {
        financeService.saveExpense(expense)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    // Limpiar la URI de la imagen seleccionada
                    selectedImageUri = null;
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

        // Si hay una imagen seleccionada, subirla primero
        if (selectedImageUri != null) {
            // Mostrar mensaje de carga
            Toast.makeText(requireContext(), "Actualizando imagen...", Toast.LENGTH_SHORT).show();

            // Si el gasto ya tenía una imagen, eliminarla primero
            if (expense.getImageUrl() != null && !expense.getImageUrl().isEmpty()) {
                servicioAlmacenamiento.eliminarArchivo(expense.getImageUrl())
                        .addOnCompleteListener(task -> {
                            // Continuar con la subida de la nueva imagen independientemente del resultado
                            uploadNewImageAndUpdate(expense);
                        });
            } else {
                // No había imagen previa, subir la nueva directamente
                uploadNewImageAndUpdate(expense);
            }
        } else {
            // No hay cambios en la imagen, actualizar el gasto directamente
            finishUpdateExpense(expense);
        }
    }

    private void uploadNewImageAndUpdate(Expense expense) {
        String userId = financeService.getCurrentUser().getUid();
        servicioAlmacenamiento.guardarArchivo(selectedImageUri, "egreso", userId)
                .addOnSuccessListener(imageUrl -> {
                    // Establecer la URL de la imagen en el gasto
                    expense.setImageUrl(imageUrl.toString());

                    // Guardar el gasto con la URL de la imagen
                    finishUpdateExpense(expense);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void finishUpdateExpense(Expense expense) {
        financeService.updateExpense(expense)
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    // Limpiar la URI de la imagen seleccionada
                    selectedImageUri = null;
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
