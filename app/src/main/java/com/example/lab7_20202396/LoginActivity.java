package com.example.lab7_20202396;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.example.lab7_20202396.databinding.ActivityLoginBinding;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager facebookCallbackManager;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inicializar Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Configurar botón de inicio de sesión
        binding.buttonLogin.setOnClickListener(v -> loginWithEmail());

        // Configurar botón de registro - Redirige a la actividad de registro
        binding.buttonRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Configurar inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        binding.buttonGoogleLogin.setOnClickListener(v -> {
            binding.progressBarLogin.setVisibility(View.VISIBLE);
            signInWithGoogle();
        });

        // Configuramos el botón de Facebook pero mostramos una alerta si se usa
        setupFacebookLogin();
    }

    private void setupFacebookLogin() {
        try {
            // Inicializar Facebook SDK
            FacebookSdk.sdkInitialize(getApplicationContext());

            facebookCallbackManager = CallbackManager.Factory.create();

            // Configuramos el botón, pero si se pulsa mostramos una alerta
            binding.buttonFacebookLogin.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Inicio de sesión con Facebook no disponible");
                builder.setMessage("Para utilizar el inicio de sesión con Facebook, necesitas " +
                        "configurar una aplicación válida en Facebook Developers Console y " +
                        "actualizar los valores de configuración en la aplicación.");
                builder.setPositiveButton("Entendido", null);
                builder.show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error configurando Facebook: " + e.getMessage());
            binding.buttonFacebookLogin.setEnabled(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Comprobar si el usuario ya está autenticado
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            startMainActivity();
        }
    }

    // Método para iniciar sesión con email y contraseña
    private void loginWithEmail() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar progreso
        binding.progressBarLogin.setVisibility(View.VISIBLE);

        // Autenticar con Firebase
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.progressBarLogin.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Inicio de sesión exitoso
                        startMainActivity();
                    } else {
                        // Si falla el inicio de sesión, mostrar mensaje
                        Toast.makeText(LoginActivity.this, "Autenticación fallida. Revise sus credenciales.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Método para iniciar sesión con Google
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Resultado de inicio de sesión con Google
        if (requestCode == RC_SIGN_IN) {
            binding.progressBarLogin.setVisibility(View.GONE);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // El inicio de sesión con Google fue exitoso, autenticar con Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Falló el inicio de sesión con Google
                binding.progressBarLogin.setVisibility(View.GONE);
                Toast.makeText(LoginActivity.this, "Error en el inicio de sesión con Google: " + e.getStatusCode(),
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Google sign in failed", e);
            }
        }
    }

    // Método para autenticar con Firebase usando credenciales de Google
    private void firebaseAuthWithGoogle(String idToken) {
        binding.progressBarLogin.setVisibility(View.VISIBLE);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    binding.progressBarLogin.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Inicio de sesión exitoso
                        startMainActivity();
                    } else {
                        // Falló el inicio de sesión
                        Toast.makeText(LoginActivity.this, "Autenticación con Google fallida.",
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "signInWithCredential:failure", task.getException());
                    }
                });
    }

    // Método para iniciar MainActivity después de autenticación exitosa
    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Cerrar LoginActivity para que el usuario no pueda volver atrás
    }
}
