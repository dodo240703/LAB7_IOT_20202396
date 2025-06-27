package com.example.lab7_20202396.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * Servicio para gestionar el almacenamiento en la nube usando Firebase Storage
 */
public class ServicioAlmacenamiento {

    private static final String STORAGE_PATH = "comprobantes/";
    private FirebaseStorage storage;
    private StorageReference storageRef;

    /**
     * Constructor que inicializa la conexión con Firebase Storage
     */
    public ServicioAlmacenamiento() {
        conectarServicio();
    }

    /**
     * Método para conectar al servicio de almacenamiento
     */
    public void conectarServicio() {
        // Inicializar Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    /**
     * Subir un archivo de imagen al almacenamiento
     *
     * @param imageUri URI de la imagen seleccionada
     * @param tipo Tipo de comprobante (ingreso o egreso)
     * @param idUsuario ID del usuario actual
     * @return Task con URL de descarga
     */
    public Task<Uri> guardarArchivo(Uri imageUri, String tipo, String idUsuario) {
        // Generar un nombre único para el archivo
        String fileName = tipo + "_" + UUID.randomUUID().toString() + ".jpg";
        String path = STORAGE_PATH + idUsuario + "/" + fileName;

        // Crear referencia al archivo en Firebase Storage
        StorageReference fileRef = storageRef.child(path);

        // Subir archivo
        return fileRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Obtener URL de descarga
                    return fileRef.getDownloadUrl();
                });
    }

    /**
     * Subir una imagen desde un bitmap al almacenamiento
     */
    public Task<Uri> guardarArchivoDesdeBitmap(Bitmap bitmap, String tipo, String idUsuario) {
        // Generar un nombre único para el archivo
        String fileName = tipo + "_" + UUID.randomUUID().toString() + ".jpg";
        String path = STORAGE_PATH + idUsuario + "/" + fileName;

        // Crear referencia al archivo en Firebase Storage
        StorageReference fileRef = storageRef.child(path);

        // Convertir bitmap a bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
        byte[] data = baos.toByteArray();

        // Subir archivo
        return fileRef.putBytes(data)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Obtener URL de descarga
                    return fileRef.getDownloadUrl();
                });
    }

    /**
     * Obtener un archivo específico por su URL y mostrarlo en un ImageView
     *
     * @param imageUrl URL del archivo a obtener
     * @param imageView ImageView donde mostrar la imagen
     */
    public void obtenerArchivo(String imageUrl, ImageView imageView) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .fit()
                    .centerCrop()
                    .into(imageView);
        }
    }

    /**
     * Descargar un archivo de imagen al dispositivo
     *
     * @param imageUrl URL de la imagen a descargar
     * @param context Contexto de la aplicación
     * @param fileName Nombre del archivo a guardar
     * @return Task con el resultado de la operación
     */
    public Task<File> descargarArchivo(String imageUrl, Context context, String fileName) {
        final File localFile = new File(context.getExternalFilesDir(null), fileName);

        return Tasks.call(() -> {
            try {
                Bitmap bitmap = Picasso.get().load(imageUrl).get();

                // Guardar bitmap a archivo local
                try (FileOutputStream out = new FileOutputStream(localFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    out.flush();
                }

                return localFile;
            } catch (Exception e) {
                throw new RuntimeException("Error al descargar la imagen: " + e.getMessage());
            }
        });
    }

    /**
     * Eliminar un archivo del almacenamiento
     *
     * @param imageUrl URL del archivo a eliminar
     * @return Task con el resultado de la operación
     */
    public Task<Void> eliminarArchivo(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("URL de imagen vacía"));
        }

        StorageReference fileRef = storage.getReferenceFromUrl(imageUrl);
        return fileRef.delete();
    }
}
