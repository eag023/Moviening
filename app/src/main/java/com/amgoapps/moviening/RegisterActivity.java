package com.amgoapps.moviening;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Actividad encargada de gestionar el registro de nuevos usuarios en la aplicación.
 * Permite el registro mediante correo electrónico y contraseña o a través de Google.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnRegister, btnGoogleRegister;
    private TextView txtGoLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference mEmailsNormalRef;
    private DatabaseReference mEmailsGoogleRef;
    private CredentialManager credentialManager;

    /**
     * Inicializa la actividad, configura las referencias a Firebase y los listeners de la interfaz.
     * @param savedInstanceState Estado guardado de la instancia.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("usernames");
        mEmailsNormalRef = FirebaseDatabase.getInstance().getReference("registered_emails");
        mEmailsGoogleRef = FirebaseDatabase.getInstance().getReference("registered_emails_google");
        credentialManager = CredentialManager.create(this);

        etUsername = findViewById(R.id.et_reg_username);
        etEmail = findViewById(R.id.et_reg_email);
        etPassword = findViewById(R.id.et_reg_password);
        etConfirmPassword = findViewById(R.id.et_reg_password_confirm);

        btnRegister = findViewById(R.id.btn_register);
        btnGoogleRegister = findViewById(R.id.btn_google_register);
        txtGoLogin = findViewById(R.id.txt_go_to_login);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        etUsername.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String username = etUsername.getText().toString().trim();
                TextInputLayout layout = (TextInputLayout) etUsername.getParent().getParent();

                if (username.isEmpty()) {
                    layout.setError(getString(R.string.write_username));
                } else {
                    mDatabase.child(username).get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult().exists()) {
                            layout.setError(getString(R.string.username_taken));
                        } else {
                            layout.setError(null);
                        }
                    });
                }
            }
        });

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String email = etEmail.getText().toString().trim();
                TextInputLayout layout = (TextInputLayout) etEmail.getParent().getParent();

                if (email.isEmpty()) {
                    layout.setError(getString(R.string.email_empty));
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    layout.setError(getString(R.string.error_format_email));
                } else {
                    layout.setError(null);
                }
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String password = etPassword.getText().toString().trim();
                TextInputLayout layout = (TextInputLayout) etPassword.getParent().getParent();
                if (password.isEmpty() || password.length() < 6) {
                    layout.setError(getString(R.string.error_password));
                } else {
                    layout.setError(null);
                }
            }
        });

        etConfirmPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String password = etPassword.getText().toString().trim();
                String confirmPassword = etConfirmPassword.getText().toString().trim();
                TextInputLayout layout = (TextInputLayout) etConfirmPassword.getParent().getParent();

                if (confirmPassword.isEmpty()) {
                    layout.setError(getString(R.string.field_empty));
                } else if (!password.equals(confirmPassword)) {
                    layout.setError(getString(R.string.error_password_matching));
                } else {
                    layout.setError(null);
                }
            }
        });

        btnRegister.setOnClickListener(v -> validarYRegistrar());
        btnGoogleRegister.setOnClickListener(v -> signInWithGoogle());

        txtGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    /**
     * Valida los campos de entrada del formulario y verifica la disponibilidad del nombre de usuario.
     */
    private void validarYRegistrar() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        TextInputLayout layoutUsername = (TextInputLayout) etUsername.getParent().getParent();
        TextInputLayout layoutEmail = (TextInputLayout) etEmail.getParent().getParent();
        TextInputLayout layoutPassword = (TextInputLayout) etPassword.getParent().getParent();
        TextInputLayout layoutConfirm = (TextInputLayout) etConfirmPassword.getParent().getParent();

        boolean hayError = false;

        if (username.isEmpty()) {
            layoutUsername.setError(getString(R.string.write_username));
            hayError = true;
        } else {
            layoutUsername.setError(null);
        }

        if (email.isEmpty()) {
            layoutEmail.setError(getString(R.string.email_empty));
            hayError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.error_format_email));
            hayError = true;
        } else {
            layoutEmail.setError(null);
        }

        if (password.isEmpty() || password.length() < 6) {
            layoutPassword.setError(getString(R.string.error_password));
            hayError = true;
        } else {
            layoutPassword.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            layoutConfirm.setError(getString(R.string.field_empty));
            hayError = true;
        } else if (!password.equals(confirmPassword)) {
            layoutConfirm.setError(getString(R.string.error_password_matching));
            hayError = true;
        } else {
            layoutConfirm.setError(null);
        }

        if (hayError) return;

        btnRegister.setEnabled(false);

        mDatabase.child(username).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                layoutUsername.setError(getString(R.string.username_taken));
                btnRegister.setEnabled(true);
            } else {
                crearCuentaEnFirebase(username, email, password);
            }
        });
    }

    /**
     * Crea una nueva cuenta en Firebase Authentication y registra los datos adicionales en Realtime Database.
     * @param username Nombre de usuario elegido.
     * @param email Correo electrónico del usuario.
     * @param password Contraseña de la cuenta.
     */
    private void crearCuentaEnFirebase(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String encodedEmail = email.replace(".", ",");
                            mEmailsNormalRef.child(encodedEmail).setValue(user.getUid());
                            mDatabase.child(username).setValue(user.getUid());

                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates).addOnCompleteListener(task1 -> {
                                Toast.makeText(RegisterActivity.this, getString(R.string.acount_created), Toast.LENGTH_SHORT).show();
                                irAMain();
                            });
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            TextInputLayout layoutEmail = (TextInputLayout) etEmail.getParent().getParent();
                            layoutEmail.setError(getString(R.string.error_email_taken));
                        } else {
                            Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Inicia el proceso de autenticación con Google utilizando el Credential Manager.
     */
    private void signInWithGoogle() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();

        credentialManager.getCredentialAsync(this, request, null, executor,
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                            String idToken = googleIdTokenCredential.getIdToken();
                            String email = googleIdTokenCredential.getId();
                            verificarExclusividadNormal(idToken, email);
                        } catch (Exception e) {
                            Log.e("Auth", "Error al procesar el token", e);
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e("Auth", "Error de Credential Manager", e);
                    }
                });
    }

    /**
     * Verifica si el correo de Google ya está registrado mediante el método tradicional antes de proceder.
     * @param idToken Token de identidad proporcionado por Google.
     * @param email Correo electrónico obtenido de la cuenta de Google.
     */
    private void verificarExclusividadNormal(String idToken, String email) {
        String encodedEmail = email.replace(".", ",");

        mEmailsNormalRef.child(encodedEmail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                mostrarErrorCuentaExistente();
            } else {
                mEmailsGoogleRef.child(encodedEmail).get().addOnCompleteListener(taskGoogle -> {
                    if (taskGoogle.isSuccessful() && taskGoogle.getResult().exists()) {
                        mostrarErrorCuentaExistente();
                    } else {
                        firebaseAuthWithGoogle(idToken);
                    }
                });
            }
        });
    }

    private void mostrarErrorCuentaExistente() {
        runOnUiThread(() -> {
            Toast.makeText(RegisterActivity.this, getString(R.string.account_exists), Toast.LENGTH_SHORT).show();
            credentialManager.clearCredentialStateAsync(new ClearCredentialStateRequest(), null, Executors.newSingleThreadExecutor(), new androidx.credentials.CredentialManagerCallback<Void, androidx.credentials.exceptions.ClearCredentialException>() {
                @Override public void onResult(Void result) {}
                @Override public void onError(androidx.credentials.exceptions.ClearCredentialException e) {}
            });
        });
    }

    /**
     * Autentica al usuario en Firebase utilizando las credenciales obtenidas de Google.
     * @param idToken Token de ID de Google.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String username = user.getDisplayName();
                    if (username == null || username.isEmpty()) {
                        if (user.getEmail() != null) {
                            username = user.getEmail().split("@")[0];
                        }
                    }

                    String email = user.getEmail();

                    if (email != null) {
                        String encodedEmail = email.replace(".", ",");
                        mEmailsGoogleRef.child(encodedEmail).setValue(user.getUid());
                    }

                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("username", username);
                    userRef.updateChildren(userData);

                    final String finalUsername = username;
                    mDatabase.child(finalUsername).setValue(user.getUid()).addOnCompleteListener(dbTask -> {
                        Toast.makeText(RegisterActivity.this, getString(R.string.google_acount_created), Toast.LENGTH_SHORT).show();
                        irAMain();
                    });
                }
            } else {
                Toast.makeText(RegisterActivity.this, getString(R.string.error_google_authentication), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Realiza la transición a la pantalla principal (MainActivity) tras un registro exitoso.
     */
    private void irAMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.putExtra("SHOW_GUIDE", true);
        startActivity(intent);
        finish();
    }
}