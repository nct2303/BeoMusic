package com.example.beomusic.views.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beomusic.R;
import com.example.beomusic.models.User;
import com.example.beomusic.repositories.AuthRepository;
import com.example.beomusic.ultis.DatabaseTestUtil;
import com.example.beomusic.views.HomeActivity;
import com.example.beomusic.views.MainActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity for user login
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private CheckBox cbRememberMe;
    private ProgressBar progressBar;
    
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Initialize repository
        authRepository = new AuthRepository(this);
        
        // Initialize views
        initViews();
        
        // Set click listeners
        setListeners();
        
        // Try auto login with saved token
        tryAutoLogin();
    }
    
    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize test connection button
        Button btnTestConnection = findViewById(R.id.btnTestConnection);
        btnTestConnection.setOnClickListener(v -> testDatabaseConnection());
    }
    
    /**
     * Test database connection
     */
    private void testDatabaseConnection() {
        progressBar.setVisibility(View.VISIBLE);
        
        // Use the DatabaseTestUtil to test connections
        DatabaseTestUtil.testAllConnections(new DatabaseTestUtil.DatabaseTestCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Show success dialog
                    new MaterialAlertDialogBuilder(LoginActivity.this)
                            .setTitle("Kết nối thành công")
                            .setMessage(message)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Show error dialog
                    new MaterialAlertDialogBuilder(LoginActivity.this)
                            .setTitle("Lỗi kết nối")
                            .setMessage(errorMessage)
                            .setPositiveButton("OK", null)
                            .show();
                });
            }
        });
    }
    
    private void setListeners() {
        // Login button click
        btnLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                loginUser();
            }
        });
        
        // Register text click
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate email
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tilEmail.setError("Email không được để trống");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }
        
        // Validate password
        String password = etPassword.getText().toString();
        if (password.isEmpty()) {
            tilPassword.setError("Mật khẩu không được để trống");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }
        
        return isValid;
    }
    
    private void loginUser() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);
        
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        boolean rememberMe = cbRememberMe.isChecked();
        
        // Call repository to login
        authRepository.loginUser(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // Hide progress
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    
                    // If remember me is not checked, clear the token after successful login
                    if (!rememberMe) {
                        user.clearRefreshToken();
                        // Update user in Firestore
                        FirebaseFirestore.getInstance().collection("users").document(user.getUserId())
                                .update("refreshToken", null, "tokenExpiry", null);
                    }
                    
                    // Navigate to main activity
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Hide progress and show error
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void tryAutoLogin() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Try to login with saved token
        authRepository.loginWithToken(new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // Navigate to main activity
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String errorMessage) {
                // Just hide progress, user needs to login manually
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    // Don't show error toast for auto-login failure
                });
            }
        });
    }
}
