package com.amgoapps.moviening;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Actividad encargada del inicio de sesión de usuarios.
 * Soporta dos métodos de autenticación mediante Firebase:
 * 1. Correo electrónico y contraseña.
 * 2. Cuenta de Google.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnGoogle;
    private TextView txtGoRegister;

    private FirebaseAuth mAuth;
    private DatabaseReference mEmailsNormalRef;

    private CredentialManager credentialManager;

    /**
     * Inicializa la interfaz, configura las opciones de Google y asigna los listeners.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mEmailsNormalRef = FirebaseDatabase.getInstance().getReference("registered_emails");
        credentialManager = CredentialManager.create(this);

        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogle = findViewById(R.id.btn_google_login);
        txtGoRegister = findViewById(R.id.txt_go_to_register);

        etEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validarEmailAlPerderFoco();
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validarPasswordAlPerderFoco();
            }
        });

        btnLogin.setOnClickListener(v -> loginUser());

        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        txtGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    /**
     * Comprueba si el texto introducido en el EditText de Email cumple las condiciones tras perder el foco
     */
    private void validarEmailAlPerderFoco() {
        String email = etEmail.getText().toString().trim();
        TextInputLayout layoutEmail = (TextInputLayout) etEmail.getParent().getParent();

        if (email.isEmpty()) {
            layoutEmail.setError(getString(R.string.field_empty));
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.error_format_email));
        } else {
            layoutEmail.setError(null);
        }
    }

    /**
     * Comprueba si el texto introducido en el EditText de Password cumple las condiciones tras perder el foco
     */
    private void validarPasswordAlPerderFoco() {
        String password = etPassword.getText().toString().trim();
        TextInputLayout layoutPassword = (TextInputLayout) etPassword.getParent().getParent();

        if (password.isEmpty()) {
            layoutPassword.setError(getString(R.string.field_empty));
        } else {
            layoutPassword.setError(null);
        }
    }

    /**
     * Realiza la autenticación mediante correo y contraseña.
     * Valida los campos y gestiona la respuesta de Firebase.
     */
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        TextInputLayout layoutEmail = (TextInputLayout) etEmail.getParent().getParent();
        TextInputLayout layoutPassword = (TextInputLayout) etPassword.getParent().getParent();

        boolean error = false;

        if (email.isEmpty()) {
            layoutEmail.setError(getString(R.string.field_empty));
            error = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.setError(getString(R.string.error_format_email));
            error = true;
        }

        if (password.isEmpty()) {
            layoutPassword.setError(getString(R.string.field_empty));
            error = true;
        }

        if (error) return;

        btnLogin.setEnabled(false);
        Toast.makeText(this, getString(R.string.loggingin), Toast.LENGTH_SHORT).show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.error_login), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Inicia el intent de Google para que el usuario seleccione su cuenta.
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

        credentialManager.getCredentialAsync(
                this,
                request,
                null,
                executor,
                new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                            String idToken = googleIdTokenCredential.getIdToken();
                            String email = googleIdTokenCredential.getId();
                            verificarMetodoNormalAntesDeLogin(idToken, email);
                        } catch (Exception e) {
                            Log.e("Auth", "Error al procesar el token", e);
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e("Auth", "Error de Credential Manager", e);
                    }
                }
        );
    }

    private void verificarMetodoNormalAntesDeLogin(String idToken, String email) {
        String encodedEmail = email.replace(".", ",");
        mEmailsNormalRef.child(encodedEmail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, getString(R.string.account_without_google), Toast.LENGTH_SHORT).show();
                    ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
                    credentialManager.clearCredentialStateAsync(clearRequest, null, Executors.newSingleThreadExecutor(), new androidx.credentials.CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override public void onResult(Void result) {}
                        @Override public void onError(ClearCredentialException e) {}
                    });
                });
            } else {
                firebaseAuthWithGoogle(idToken);
            }
        });
    }

    /**
     * Permite que Firebase trate al usuario de Google como un usuario registrado en la app.
     *
     * @param idToken El token de seguridad proporcionado por Google.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNewUser) {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, getString(R.string.account_no_exists), Toast.LENGTH_SHORT).show());
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.delete().addOnCompleteListener(deleteTask -> {
                                    ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
                                    credentialManager.clearCredentialStateAsync(clearRequest, null, Executors.newSingleThreadExecutor(), new androidx.credentials.CredentialManagerCallback<Void, ClearCredentialException>() {
                                        @Override
                                        public void onResult(Void result) { Log.d("Auth", "Credenciales limpias"); }
                                        @Override
                                        public void onError(ClearCredentialException e) { Log.e("Auth", "Error", e); }
                                    });
                                });
                            }
                        } else {
                            runOnUiThread(() -> Toast.makeText(LoginActivity.this, getString(R.string.loggingin), Toast.LENGTH_SHORT).show());
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.error_google_authentication), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}