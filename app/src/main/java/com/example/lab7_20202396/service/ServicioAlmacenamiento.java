package com.example.lab7_20202396.service;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

/**
 * Servicio para manejar la subida de archivos a Firebase Storage
 */
public class ServicioAlmacenamiento {

    private static final String TAG = "ServicioAlmacenamiento";
    private final FirebaseStorage storage;
    private final StorageReference storageRef;

    public ServicioAlmacenamiento() {
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    /**
     * Guarda un archivo en Firebase Storage
     * @param fileUri URI del archivo a subir
     * @param carpeta Carpeta donde guardar (ej: "egreso", "ingreso")
     * @param userId ID del usuario
     * @return Task con la URL de descarga del archivo
     */
    public Task<Uri> guardarArchivo(Uri fileUri, String carpeta, String userId) {
        // Generar nombre único para el archivo
        String fileName = UUID.randomUUID().toString() + ".jpg";

        // Crear referencia al archivo en Storage
        StorageReference fileRef = storageRef
                .child("images")
                .child(carpeta)
                .child(userId)
                .child(fileName);

        Log.d(TAG, "Subiendo archivo a: " + fileRef.getPath());

        // Subir archivo
        UploadTask uploadTask = fileRef.putFile(fileUri);

        // Retornar la URL de descarga después de la subida
        return uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            // Obtener URL de descarga
            return fileRef.getDownloadUrl();
        });
    }

    /**
     * Elimina un archivo de Firebase Storage
     * @param imageUrl URL del archivo a eliminar
     * @return Task de eliminación
     */
    public Task<Void> eliminarArchivo(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forException(
                new IllegalArgumentException("URL de imagen no válida"));
        }

        try {
            StorageReference fileRef = storage.getReferenceFromUrl(imageUrl);
            return fileRef.delete();
        } catch (Exception e) {
            Log.e(TAG, "Error al eliminar archivo: " + e.getMessage());
            return com.google.android.gms.tasks.Tasks.forException(e);
        }
    }
}
