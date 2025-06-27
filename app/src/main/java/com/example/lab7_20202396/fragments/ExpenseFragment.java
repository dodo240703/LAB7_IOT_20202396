package com.example.lab7_20202396.fragments;

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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lab7_20202396.R;
import com.example.lab7_20202396.databinding.FragmentExpenseBinding;
import com.example.lab7_20202396.model.Expense;
import com.example.lab7_20202396.service.ServicioAlmacenamiento;
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

    // Variables para manejo de imágenes
    private ServicioAlmacenamiento servicioAlmacenamiento;
    private Uri selectedImageUri = null;
    private TextView tvImageStatus;
    private View currentDialogView;
    private static final String TAG = "ExpenseFragment";

    // Lanzadores para permisos y selección de imágenes
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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
        servicioAlmacenamiento = new ServicioAlmacenamiento();
        expenseList = new ArrayList<>();

        setupImagePickers();
        setupRecyclerView();

        binding.fabAddExpense.setOnClickListener(v -> showAddExpenseDialog(null));

        loadExpenses();
    }

    private void setupImagePickers() {
        // Inicializar lanzador de permisos
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(requireContext(),
                                "Se requiere permiso para acceder a las imágenes. Por favor, otorga el permiso manualmente en Configuración > Aplicaciones.",
                                Toast.LENGTH_LONG).show();
                    }
                });

        // Inicializar lanzador de selección de imágenes
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            selectedImageUri = data.getData();
                            Log.d(TAG, "Imagen seleccionada: " + selectedImageUri);

                            // Actualizar UI si el diálogo sigue visible
                            if (currentDialogView != null && tvImageStatus != null) {
                                tvImageStatus.setText("Imagen seleccionada");
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "No se seleccionó ninguna imagen", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean hasImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 12 y anteriores
            return ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            // Android 12 y anteriores
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");

            Log.d(TAG, "Lanzando intent de selección de imagen");
            Toast.makeText(requireContext(), "Abriendo galería...", Toast.LENGTH_SHORT).show();

            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir selector de imágenes: " + e.getMessage(), e);

            try {
                // Método alternativo usando ACTION_GET_CONTENT
                Intent fallbackIntent = new Intent(Intent.ACTION_GET_CONTENT);
                fallbackIntent.setType("image/*");
                fallbackIntent.addCategory(Intent.CATEGORY_OPENABLE);

                imagePickerLauncher.launch(
                        Intent.createChooser(fallbackIntent, "Seleccionar imagen")
                );

                Log.d(TAG, "Usando intent alternativo ACTION_GET_CONTENT");
            } catch (Exception e2) {
                Log.e(TAG, "Error con intent alternativo: " + e2.getMessage(), e2);
                Toast.makeText(requireContext(),
                        "Error al abrir la galería. Verifica que tengas una aplicación de galería instalada.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new ExpenseAdapter(expenseList,
                expense -> showAddExpenseDialog(expense),  // Edit click listener
                expense -> showDeleteConfirmationDialog(expense),  // Delete click listener
                this  // Pasar referencia del fragmento
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

        EditText editTextTitle = dialogView.findViewById(R.id.etTitle);
        EditText editTextAmount = dialogView.findViewById(R.id.etAmount);
        EditText editTextDescription = dialogView.findViewById(R.id.etDescription);
        EditText editTextDate = dialogView.findViewById(R.id.etDate);
        tvImageStatus = dialogView.findViewById(R.id.tvImageStatus);

        // Si estamos editando, llenar campos y deshabilitar título según requisitos
        if (isEdit) {
            editTextTitle.setText(expense.getTitle());
            editTextTitle.setEnabled(false); // No se puede editar el título
            editTextAmount.setText(String.valueOf(expense.getAmount()));
            editTextDescription.setText(expense.getDescription());
            editTextDate.setText(dateFormat.format(expense.getDate()));
            editTextDate.setEnabled(false); // No se puede editar la fecha

            // Cargar estado de la imagen
            if (expense.getImageUrl() != null) {
                tvImageStatus.setText("Imagen cargada");
            } else {
                tvImageStatus.setText("Sin imagen");
            }
        } else {
            // Para nuevo egreso, colocar fecha actual
            editTextDate.setText(dateFormat.format(new Date()));
            tvImageStatus.setText("Sin imagen");
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
        currentDialogView = dialogView; // Guardar referencia a la vista del diálogo

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

        // Manejar clic en el estado de la imagen
        tvImageStatus.setOnClickListener(v -> {
            if (isEdit) {
                // En editar, solo permitir ver la imagen
                viewImage(expense.getImageUrl());
            } else {
                // En nuevo, solicitar permiso y abrir selector de imagen
                if (hasImagePermission()) {
                    openImagePicker();
                } else {
                    requestImagePermission();
                }
            }
        });

        // Configurar botón de seleccionar imagen si existe en el layout
        View btnSelectImage = dialogView.findViewById(R.id.btnSelectImage);
        if (btnSelectImage != null) {
            btnSelectImage.setOnClickListener(v -> {
                // Verificar y solicitar permiso de lectura de almacenamiento
                if (hasImagePermission()) {
                    openImagePicker();
                } else {
                    requestImagePermission();
                }
            });
        }
    }

    private void saveExpense(String title, double amount, String description, Date date) {
        if (auth.getCurrentUser() != null) {
            showLoading(true);

            String userId = auth.getCurrentUser().getUid();
            String id = db.collection("expenses").document().getId();
            long timestamp = date.getTime();

            // Si hay una imagen seleccionada, subirla primero
            if (selectedImageUri != null) {
                Toast.makeText(requireContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show();

                servicioAlmacenamiento.guardarArchivo(selectedImageUri, "egreso", userId)
                        .addOnSuccessListener(imageUrl -> {
                            // Crear el gasto con la URL de la imagen
                            Expense expense = new Expense(id, title, amount, description, timestamp, userId, imageUrl.toString());
                            saveExpenseToFirestore(expense);
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                // No hay imagen, crear el gasto sin imagen
                Expense expense = new Expense(id, title, amount, description, timestamp, userId, null);
                saveExpenseToFirestore(expense);
            }
        }
    }

    private void saveExpenseToFirestore(Expense expense) {
        db.collection("expenses")
                .document(expense.getId())
                .set(expense)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), R.string.expense_saved, Toast.LENGTH_SHORT).show();
                    selectedImageUri = null; // Limpiar selección de imagen
                    loadExpenses();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExpense(Expense expense, double newAmount, String newDescription) {
        if (auth.getCurrentUser() != null) {
            showLoading(true);

            // Si hay una nueva imagen seleccionada, subirla primero
            if (selectedImageUri != null) {
                String userId = auth.getCurrentUser().getUid();
                servicioAlmacenamiento.guardarArchivo(selectedImageUri, "egreso", userId)
                        .addOnSuccessListener(imageUrl -> {
                            // Eliminar imagen anterior si existe
                            if (expense.getImageUrl() != null && !expense.getImageUrl().isEmpty()) {
                                servicioAlmacenamiento.eliminarArchivo(expense.getImageUrl());
                            }

                            // Actualizar con nueva imagen
                            updateExpenseInFirestore(expense, newAmount, newDescription, imageUrl.toString());
                        })
                        .addOnFailureListener(e -> {
                            showLoading(false);
                            Toast.makeText(requireContext(), "Error al subir la imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                // No hay nueva imagen, actualizar solo los campos básicos
                updateExpenseInFirestore(expense, newAmount, newDescription, expense.getImageUrl());
            }
        }
    }

    private void updateExpenseInFirestore(Expense expense, double newAmount, String newDescription, String imageUrl) {
        db.collection("expenses")
                .document(expense.getId())
                .update(
                        "amount", newAmount,
                        "description", newDescription,
                        "imageUrl", imageUrl
                )
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), R.string.expense_updated, Toast.LENGTH_SHORT).show();
                    selectedImageUri = null; // Limpiar selección de imagen
                    loadExpenses();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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

    private void viewImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(requireContext(), "No hay imagen para mostrar", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear diálogo para mostrar opciones de imagen
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Opciones de imagen")
                .setItems(new String[]{"Ver imagen", "Descargar imagen", "Copiar URL"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openImageViewer(imageUrl);
                            break;
                        case 1:
                            downloadImage(imageUrl);
                            break;
                        case 2:
                            copyImageUrl(imageUrl);
                            break;
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    public void openImageViewer(String imageUrl) {
        try {
            // Opción 1: Abrir con navegador web
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
            startActivity(browserIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir imagen en navegador: " + e.getMessage());

            try {
                // Opción 2: Intent genérico para ver imagen
                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.setDataAndType(Uri.parse(imageUrl), "image/*");
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(viewIntent, "Ver imagen con"));
            } catch (Exception e2) {
                Log.e(TAG, "Error al abrir imagen: " + e2.getMessage());

                // Fallback: Mostrar diálogo con imagen en WebView
                showImageInDialog(imageUrl);
            }
        }
    }

    private void showImageInDialog(String imageUrl) {
        // Crear WebView para mostrar la imagen
        android.webkit.WebView webView = new android.webkit.WebView(requireContext());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);

        // Cargar la imagen
        String htmlData = "<html><body style='margin:0;padding:0;text-align:center;'>" +
                "<img src='" + imageUrl + "' style='max-width:100%;height:auto;' />" +
                "</body></html>";
        webView.loadData(htmlData, "text/html", "UTF-8");

        // Mostrar en diálogo
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Comprobante de gasto")
                .setView(webView)
                .setPositiveButton("Cerrar", null)
                .setNeutralButton("Abrir en navegador", (dialog, which) -> {
                    try {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
                        startActivity(browserIntent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "No se puede abrir en navegador", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void downloadImage(String imageUrl) {
        try {
            // Usar DownloadManager de Android
            android.app.DownloadManager downloadManager =
                    (android.app.DownloadManager) requireContext().getSystemService(android.content.Context.DOWNLOAD_SERVICE);

            if (downloadManager != null) {
                // Crear nombre único para el archivo
                String fileName = "comprobante_" + System.currentTimeMillis() + ".jpg";

                android.app.DownloadManager.Request request =
                        new android.app.DownloadManager.Request(Uri.parse(imageUrl));

                request.setTitle("Descargando comprobante");
                request.setDescription("Descargando imagen del gasto");
                request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
                request.setAllowedNetworkTypes(android.app.DownloadManager.Request.NETWORK_WIFI |
                        android.app.DownloadManager.Request.NETWORK_MOBILE);

                long downloadId = downloadManager.enqueue(request);

                Toast.makeText(requireContext(),
                        "Descarga iniciada. Revisa la carpeta de Descargas.",
                        Toast.LENGTH_LONG).show();

                Log.d(TAG, "Descarga iniciada con ID: " + downloadId);
            } else {
                throw new Exception("DownloadManager no disponible");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al descargar imagen: " + e.getMessage());

            // Fallback: Abrir en navegador para descarga manual
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
                startActivity(browserIntent);
                Toast.makeText(requireContext(),
                        "Abriendo en navegador. Puedes descargar manualmente.",
                        Toast.LENGTH_LONG).show();
            } catch (Exception e2) {
                Toast.makeText(requireContext(),
                        "Error al descargar: " + e2.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void copyImageUrl(String imageUrl) {
        try {
            android.content.ClipboardManager clipboard =
                    (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);

            if (clipboard != null) {
                android.content.ClipData clip = android.content.ClipData.newPlainText("URL de imagen", imageUrl);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "URL copiada al portapapeles", Toast.LENGTH_SHORT).show();
            } else {
                throw new Exception("Clipboard no disponible");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al copiar URL: " + e.getMessage());
            Toast.makeText(requireContext(), "Error al copiar URL", Toast.LENGTH_SHORT).show();
        }
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
        private final ExpenseFragment fragment; // Referencia al fragmento para acceder a los métodos de imagen

        public ExpenseAdapter(List<Expense> expenses, ExpenseListener editListener, ExpenseListener deleteListener, ExpenseFragment fragment) {
            this.expenses = expenses;
            this.editListener = editListener;
            this.deleteListener = deleteListener;
            this.fragment = fragment;
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
            holder.bind(expense, editListener, deleteListener, fragment);
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

            public void bind(Expense expense, ExpenseListener editListener, ExpenseListener deleteListener, ExpenseFragment fragment) {
                // Configurar vistas del ítem
                android.widget.TextView titleTextView = itemView.findViewById(R.id.textViewExpenseTitle);
                android.widget.TextView amountTextView = itemView.findViewById(R.id.textViewExpenseAmount);
                android.widget.TextView descriptionTextView = itemView.findViewById(R.id.textViewExpenseDescription);
                android.widget.TextView dateTextView = itemView.findViewById(R.id.textViewExpenseDate);
                android.widget.Button editButton = itemView.findViewById(R.id.buttonEditExpense);
                android.widget.Button deleteButton = itemView.findViewById(R.id.buttonDeleteExpense);

                // Botones para imagen
                android.widget.Button viewReceiptButton = itemView.findViewById(R.id.buttonViewReceipt);
                android.widget.Button downloadImageButton = itemView.findViewById(R.id.buttonDownloadImage);

                // Configurar datos básicos
                if (titleTextView != null) titleTextView.setText(expense.getTitle());
                if (amountTextView != null) amountTextView.setText(String.format(Locale.getDefault(), "S/ %.2f", expense.getAmount()));
                if (descriptionTextView != null) descriptionTextView.setText(expense.getDescription());
                if (dateTextView != null) dateTextView.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(expense.getDate()));

                // Configurar botones básicos
                if (editButton != null) editButton.setOnClickListener(v -> editListener.onExpenseClick(expense));
                if (deleteButton != null) deleteButton.setOnClickListener(v -> deleteListener.onExpenseClick(expense));

                // Configurar botones de imagen
                boolean hasImage = expense.getImageUrl() != null && !expense.getImageUrl().isEmpty();

                if (viewReceiptButton != null) {
                    if (hasImage) {
                        viewReceiptButton.setVisibility(View.VISIBLE);
                        viewReceiptButton.setEnabled(true);
                        viewReceiptButton.setText("VER COMPROBANTE");
                        viewReceiptButton.setOnClickListener(v -> fragment.openImageViewer(expense.getImageUrl()));
                    } else {
                        viewReceiptButton.setVisibility(View.GONE);
                    }
                }

                if (downloadImageButton != null) {
                    if (hasImage) {
                        downloadImageButton.setVisibility(View.VISIBLE);
                        downloadImageButton.setEnabled(true);
                        downloadImageButton.setText("DESCARGAR IMAGEN");
                        downloadImageButton.setOnClickListener(v -> fragment.downloadImage(expense.getImageUrl()));
                    } else {
                        downloadImageButton.setVisibility(View.GONE);
                    }
                }
            }
        }
    }
}
