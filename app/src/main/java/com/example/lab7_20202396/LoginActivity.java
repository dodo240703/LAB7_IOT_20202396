package com.example.lab7_20202396;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lab7_20202396.databinding.ActivityLoginBinding;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
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

        // Configurar botón de registro
        binding.buttonRegister.setOnClickListener(v -> registerUser());

        // Configurar inicio de sesión con Google
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        binding.buttonGoogleLogin.setOnClickListener(v -> signInWithGoogle());

        // Configurar inicio de sesión con Facebook
        facebookCallbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = new LoginButton(this);
        loginButton.setPermissions("email", "public_profile");
        loginButton.registerCallback(facebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                // Cancelado por usuario
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        binding.buttonFacebookLogin.setOnClickListener(v -> loginButton.performClick());
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

    private void loginWithEmail() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.setError(getString(R.string.required_field));
            return;
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.setError(getString(R.string.required_field));
            return;
        }

        showProgress(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            // Inicio de sesión exitoso
                            startMainActivity();
                        } else {
                            // Si el inicio de sesión falla, mostrar un mensaje al usuario
                            Toast.makeText(LoginActivity.this, getString(R.string.login_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void registerUser() {
        String email = binding.editTextEmail.getText().toString().trim();
        String password = binding.editTextPassword.getText().toString().trim();

        if (email.isEmpty()) {
            binding.textInputLayoutEmail.setError(getString(R.string.required_field));
            return;
        }

        if (password.isEmpty()) {
            binding.textInputLayoutPassword.setError(getString(R.string.required_field));
            return;
        }

        showProgress(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            // Registro exitoso
                            Toast.makeText(LoginActivity.this, getString(R.string.register_success),
                                    Toast.LENGTH_SHORT).show();
                            startMainActivity();
                        } else {
                            // Si el registro falla, mostrar un mensaje al usuario
                            Toast.makeText(LoginActivity.this, getString(R.string.register_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Resultado para inicio de sesión con Facebook
        facebookCallbackManager.onActivityResult(requestCode, resultCode, data);

        // Resultado para inicio de sesión con Google
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Error de Google: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showProgress(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            startMainActivity();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error de autenticación con Google",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        showProgress(true);
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showProgress(false);
                        if (task.isSuccessful()) {
                            startMainActivity();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error de autenticación con Facebook",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showProgress(boolean show) {
        binding.progressBarLogin.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
