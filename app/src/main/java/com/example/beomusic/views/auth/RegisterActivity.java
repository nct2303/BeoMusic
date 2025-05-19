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
import com.example.beomusic.ultis.PasswordUtils;
import com.example.beomusic.views.MainActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Activity for user registration
 */
public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilUsername, tilEmail, tilPassword, tilConfirmPassword, tilFullName;
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword, etFullName;
    private Button btnRegister;
    private TextView tvLogin;
    private CheckBox cbTerms;
    private ProgressBar progressBar;
    
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        
        // Initialize repository
        authRepository = new AuthRepository(this);
        
        // Initialize views
        initViews();
        
        // Set click listeners
        setListeners();
    }
    
    private void initViews() {
        tilUsername = findViewById(R.id.tilUsername);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilFullName = findViewById(R.id.tilFullName);
        
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etFullName = findViewById(R.id.etFullName);
        
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        cbTerms = findViewById(R.id.cbTerms);
        progressBar = findViewById(R.id.progressBar);
    }
    
    private void setListeners() {
        // Register button click
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });
        
        // Login text click
        tvLogin.setOnClickListener(v -> {
            finish(); // Go back to login activity
        });
    }
    
    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate username
        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            tilUsername.setError("Tên người dùng không được để trống");
            isValid = false;
        } else if (username.length() < 3) {
            tilUsername.setError("Tên người dùng phải có ít nhất 3 ký tự");
            isValid = false;
        } else {
            tilUsername.setError(null);
        }
        
        // Validate email
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            tilEmail.setError("Email không được để trống");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }
        
        // Validate full name
        String fullName = etFullName.getText().toString().trim();
        if (fullName.isEmpty()) {
            tilFullName.setError("Họ tên không được để trống");
            isValid = false;
        } else {
            tilFullName.setError(null);
        }
        
        // Validate password
        String password = etPassword.getText().toString();
        if (password.isEmpty()) {
            tilPassword.setError("Mật khẩu không được để trống");
            isValid = false;
        } else if (!PasswordUtils.isPasswordSecure(password)) {
            tilPassword.setError("Mật khẩu phải có ít nhất 8 ký tự và 3 trong 4 loại: chữ hoa, chữ thường, số, ký tự đặc biệt");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }
        
        // Validate confirm password
        String confirmPassword = etConfirmPassword.getText().toString();
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Xác nhận mật khẩu không được để trống");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }
        
        // Validate terms agreement
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Bạn phải đồng ý với điều khoản sử dụng", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        return isValid;
    }
    
    private void registerUser() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);
        
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String fullName = etFullName.getText().toString().trim();
        
        // Call repository to register
        authRepository.registerUser(email, password, username, fullName, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // Hide progress
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    
                    // Show success message
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to main activity
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
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
                    btnRegister.setEnabled(true);
                    Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
