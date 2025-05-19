package com.example.beomusic.views.auth;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Switch;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.beomusic.R;
import com.example.beomusic.models.User;
import com.example.beomusic.repositories.AuthRepository;
import com.example.beomusic.views.HomeActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Activity for managing user account
 */
public class AccountActivity extends AppCompatActivity {

    private TextView tvUsername, tvEmail, tvFullName;
    private Button btnLogout, btnDeleteAccount, btnEditProfile;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private Switch switchAutoLogin;
    
    private AuthRepository authRepository;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        
        // Initialize repository
        authRepository = new AuthRepository(this);
        
        // Initialize views
        initViews();
        
        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tài khoản");
        }
        
        // Set click listeners
        setListeners();
        
        // Load user data
        loadUserData();
    }
    
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvFullName = findViewById(R.id.tvFullName);
        btnLogout = findViewById(R.id.btnLogout);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        progressBar = findViewById(R.id.progressBar);
        switchAutoLogin = findViewById(R.id.switchAutoLogin);
        
        // Initialize switch state
        boolean isAutoLoginEnabled = authRepository.isAutoLoginEnabled();
        switchAutoLogin.setChecked(isAutoLoginEnabled);
    }
    
    private void setListeners() {
        // Logout button click
        btnLogout.setOnClickListener(v -> {
            logoutUser();
        });
        
        // Delete account button click
        btnDeleteAccount.setOnClickListener(v -> {
            showDeleteAccountConfirmation();
        });
        
        // Edit profile button click
        btnEditProfile.setOnClickListener(v -> {
            showEditProfileDialog();
        });

        // Auto-login switch change
        switchAutoLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Show alert dialog
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Tự động đăng nhập")
                    .setMessage(isChecked ? 
                            "auto login core ON" :
                            "auto login core OFF")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // Save the auto-login setting
                        authRepository.setAutoLoginEnabled(isChecked);
                        
                        // If auto-login is disabled, clear saved credentials
                        if (!isChecked) {
                            authRepository.clearSavedCredentials();
                        }
                    })
                    .setCancelable(false) // Prevent dialog from being dismissed by clicking outside
                    .show();
        });
    }
    
    private void loadUserData() {
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Get current Firebase user
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            // Not logged in, go to login screen
            navigateToLogin();
            return;
        }
        
        // Get user data from Firestore
        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    
                    currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null) {
                        // Display user data
                        tvUsername.setText("Tên người dùng: " + currentUser.getUsername());
                        tvEmail.setText("Email: " + currentUser.getEmail());
                        tvFullName.setText("Họ và tên: " + currentUser.getFullName());
                        
                        // Format and display dates
                        TextView tvCreatedAt = findViewById(R.id.tvCreatedAt);
                        TextView tvLastLogin = findViewById(R.id.tvLastLogin);
                        
                        // Format the dates
                        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault());
                        
                        // Display account creation date
                        if (currentUser.getCreatedAt() != null) {
                            tvCreatedAt.setText("Ngày tạo tài khoản: " + dateFormat.format(currentUser.getCreatedAt()));
                        } else {
                            tvCreatedAt.setText("Ngày tạo tài khoản: Không có thông tin");
                        }
                        
                        // Display last login date
                        if (currentUser.getLastLogin() != null) {
                            tvLastLogin.setText("Đăng nhập lần cuối: " + dateFormat.format(currentUser.getLastLogin()));
                        } else {
                            tvLastLogin.setText("Đăng nhập lần cuối: Không có thông tin");
                        }
                        
                    } else {
                        Toast.makeText(this, "Không thể tải thông tin người dùng", Toast.LENGTH_SHORT).show();
                        navigateToLogin();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
    }
    
    private void logoutUser() {
        // Show confirmation dialog
        new MaterialAlertDialogBuilder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    // Perform logout
                    authRepository.logout();
                    navigateToLogin();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showDeleteAccountConfirmation() {
        // Show confirmation dialog
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa tài khoản")
                .setMessage("Bạn có chắc chắn muốn xóa tài khoản? Hành động này không thể hoàn tác và tất cả dữ liệu của bạn sẽ bị xóa vĩnh viễn.")
                .setPositiveButton("Xóa tài khoản", (dialog, which) -> {
                    // Show password confirmation dialog
                    showPasswordConfirmationDialog();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void showPasswordConfirmationDialog() {
        // Inflate custom dialog layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_password_confirm, null);
        TextInputEditText etPassword = view.findViewById(R.id.etPassword);
        
        // Create and show dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận mật khẩu")
                .setView(view)
                .setPositiveButton("Xác nhận", null) // Set listener later to prevent auto-dismiss
                .setNegativeButton("Hủy", null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button button = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String password = etPassword.getText().toString();
                if (password.isEmpty()) {
                    etPassword.setError("Vui lòng nhập mật khẩu");
                    return;
                }
                
                // Validate password here
                validatePassword(password, dialog);
            });
        });
        
        dialog.show();
    }
    
    private void validatePassword(String password, AlertDialog dialog) {
        authRepository.validatePassword(currentUser, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                dialog.dismiss();
                requestAccountDeletion(password);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AccountActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void requestAccountDeletion(String password) {
        if (currentUser == null) {
            Toast.makeText(this, "Không có thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);

        // Call repository to request account deletion
        authRepository.deleteAccount(currentUser, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AccountActivity.this, "Yêu cầu xóa tài khoản đã được gửi thành công", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AccountActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void showEditProfileDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "Không có thông tin người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Inflate custom dialog layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_profile, null);
        TextInputEditText etUsername = view.findViewById(R.id.etUsername);
        TextInputEditText etFullName = view.findViewById(R.id.etFullName);
        
        // Pre-fill current values
        etUsername.setText(currentUser.getUsername());
        etFullName.setText(currentUser.getFullName());
        
        // Create and show dialog
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Chỉnh sửa hồ sơ")
                .setView(view)
                .setPositiveButton("Lưu", null) // Set listener later to prevent auto-dismiss
                .setNegativeButton("Hủy", null)
                .create();
        
        dialog.setOnShowListener(dialogInterface -> {
            Button button = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String username = etUsername.getText().toString().trim();
                String fullName = etFullName.getText().toString().trim();
                
                // Validate inputs
                boolean isValid = true;
                if (username.isEmpty()) {
                    etUsername.setError("Tên người dùng không được để trống");
                    isValid = false;
                } else if (username.length() < 3) {
                    etUsername.setError("Tên người dùng phải có ít nhất 3 ký tự");
                    isValid = false;
                }
                
                if (fullName.isEmpty()) {
                    etFullName.setError("Họ tên không được để trống");
                    isValid = false;
                }
                
                if (!isValid) {
                    return;
                }
                
                // Dismiss dialog and update profile
                dialog.dismiss();
                updateProfile(username, fullName);
            });
        });
        
        dialog.show();
    }
    
    private void updateProfile(String username, String fullName) {
        if (currentUser == null) {
            return;
        }
        
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        
        // Update user in Firestore
        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUserId())
                .update("username", username, "fullName", fullName)
                .addOnSuccessListener(aVoid -> {
                    // Update local user object
                    currentUser.setUsername(username);
                    currentUser.setFullName(fullName);
                    
                    // Update UI
                    tvUsername.setText("Tên người dùng: " + username);
                    tvFullName.setText("Họ và tên: " + fullName);
                    
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        navigateToHome();
        return true;
    }

    private void navigateToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
