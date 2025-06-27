package com.example.lab7_20202396;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class FinanceApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Inicializar Firebase
        FirebaseApp.initializeApp(this);

        // Activar persistencia offline (opcional pero recomendado)
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
