package com.example.beomusic.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;

import com.example.beomusic.models.AuthToken;
import com.example.beomusic.models.User;
import com.example.beomusic.ultis.PasswordUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.UUID;

/**
 * Repository class for handling authentication operations
 */
public class AuthRepository {
    private static final String PREF_NAME = "BeoMusicPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_AUTO_LOGIN = "autoLogin";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final SharedPreferences sharedPreferences;

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String errorMessage);
    }

    public AuthRepository(Context context) {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Check if auto-login is enabled
     * @return true if auto-login is enabled, false otherwise
     */
    public boolean isAutoLoginEnabled() {
        return sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false);
    }

    /**
     * Set auto-login enabled/disabled
     * @param enabled true to enable auto-login, false to disable
     */
    public void setAutoLoginEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_AUTO_LOGIN, enabled);
        editor.apply();
    }

    /**
     * Register a new user
     * @param email User email
     * @param password User password
     * @param username Username
     * @param fullName User's full name
     * @param callback Callback for result
     */
    public void registerUser(String email, String password, String username, String fullName, AuthCallback callback) {
        // Check if password is secure
        if (!PasswordUtils.isPasswordSecure(password)) {
            callback.onError("Mật khẩu không đủ mạnh. Cần ít nhất 8 ký tự và 3 trong 4 loại: chữ hoa, chữ thường, số, ký tự đặc biệt.");
            return;
        }

        // Create user with Firebase Authentication
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            
                            // Create User object
                            User newUser = new User(userId, username, email, fullName);
                            
                            // Hash password for storage
                            String hashedPassword = PasswordUtils.hashPasswordForStorage(password);
                            newUser.setPasswordHash(hashedPassword);
                            
                            // Save user to Firestore
                            firestore.collection("users").document(userId)
                                    .set(newUser)
                                    .addOnSuccessListener(aVoid -> {
                                        // Generate and save auth token
                                        generateAndSaveAuthToken(newUser, callback);
                                    })
                                    .addOnFailureListener(e -> {
                                        // Delete Firebase Auth user if Firestore save fails
                                        firebaseUser.delete();
                                        callback.onError("Đăng ký thất bại: " + e.getMessage());
                                    });
                        } else {
                            callback.onError("Đăng ký thất bại: Không thể tạo người dùng");
                        }
                    } else {
                        callback.onError("Đăng ký thất bại: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"));
                    }
                });
    }

    /**
     * Login user with email and password
     * @param email User email
     * @param password User password
     * @param callback Callback for result
     */
    public void loginUser(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();
                            
                            // Get user from Firestore
                            firestore.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        User user = documentSnapshot.toObject(User.class);
                                        if (user != null) {
                                            // Update last login time
                                            user.setLastLogin(new Date());
                                            
                                            // Generate and save auth token
                                            generateAndSaveAuthToken(user, callback);
                                        } else {
                                            callback.onError("Đăng nhập thất bại: Không tìm thấy thông tin người dùng");
                                        }
                                    })
                                    .addOnFailureListener(e -> 
                                        callback.onError("Đăng nhập thất bại: " + e.getMessage())
                                    );
                        } else {
                            callback.onError("Đăng nhập thất bại: Không thể xác thực người dùng");
                        }
                    } else {
                        callback.onError("Đăng nhập thất bại: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Sai email hoặc mật khẩu"));
                    }
                });
    }

    /**
     * Login with saved token (auto login)
     * @param callback Callback for result
     */
    public void loginWithToken(AuthCallback callback) {
        String userId = sharedPreferences.getString(KEY_USER_ID, null);
        String token = sharedPreferences.getString(KEY_AUTH_TOKEN, null);
        
        if (userId == null || token == null) {
            callback.onError("Không có thông tin đăng nhập được lưu");
            return;
        }
        
        // Verify token in Firestore
        firestore.collection("auth_tokens").document(token)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    AuthToken authToken = documentSnapshot.toObject(AuthToken.class);
                    if (authToken != null && authToken.isValid() && authToken.getUserId().equals(userId)) {
                        // Token is valid, get user
                        firestore.collection("users").document(userId)
                                .get()
                                .addOnSuccessListener(userDoc -> {
                                    User user = userDoc.toObject(User.class);
                                    if (user != null) {
                                        // Update last login time
                                        user.setLastLogin(new Date());
                                        firestore.collection("users").document(userId).update("lastLogin", new Date());
                                        
                                        callback.onSuccess(user);
                                    } else {
                                        clearSavedCredentials();
                                        callback.onError("Không tìm thấy thông tin người dùng");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    clearSavedCredentials();
                                    callback.onError("Lỗi khi lấy thông tin người dùng: " + e.getMessage());
                                });
                    } else {
                        // Token is invalid or expired
                        clearSavedCredentials();
                        callback.onError("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
                    }
                })
                .addOnFailureListener(e -> {
                    clearSavedCredentials();
                    callback.onError("Lỗi xác thực: " + e.getMessage());
                });
    }

    /**
     * Logout user
     */
    public void logout() {
        String token = sharedPreferences.getString(KEY_AUTH_TOKEN, null);
        if (token != null) {
            // Delete token from Firestore
            firestore.collection("auth_tokens").document(token).delete();
        }
        
        // Clear saved credentials
        clearSavedCredentials();
        
        // Sign out from Firebase Auth
        firebaseAuth.signOut();
    }

    /**
     * Delete user account
     * @param user User to delete
     * @param password Current password for verification
     * @param callback Callback for result
     */
    public void deleteAccount(User user, String password, AuthCallback callback) {
        // Re-authenticate user before deletion
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            callback.onError("Không có người dùng đang đăng nhập");
            return;
        }
        
        // Verify password from Firestore
        firestore.collection("users").document(user.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User storedUser = documentSnapshot.toObject(User.class);
                    if (storedUser != null && storedUser.getPasswordHash() != null) {
                        // Verify password
                        if (PasswordUtils.verifyPassword(password, storedUser.getPasswordHash())) {
                            // Delete user data from Firestore
                            deleteUserData(user.getUserId(), () -> {
                                // Delete Firebase Auth user
                                firebaseUser.delete()
                                        .addOnSuccessListener(aVoid -> {
                                            clearSavedCredentials();
                                            callback.onSuccess(null);
                                        })
                                        .addOnFailureListener(e -> 
                                            callback.onError("Xóa tài khoản thất bại: " + e.getMessage())
                                        );
                            });
                        } else {
                            callback.onError("Mật khẩu không chính xác");
                        }
                    } else {
                        callback.onError("Không thể xác minh người dùng");
                    }
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi xác minh người dùng: " + e.getMessage())
                );
    }

    /**
     * Generate and save authentication token
     * @param user User to generate token for
     * @param callback Callback for result
     */
    private void generateAndSaveAuthToken(User user, AuthCallback callback) {
        // Generate unique token
        String tokenId = UUID.randomUUID().toString();
        
        // Get device info
        String deviceInfo = getDeviceInfo();
        
        // Create auth token
        AuthToken authToken = new AuthToken(user.getUserId(), tokenId, deviceInfo);
        
        // Save token to Firestore
        firestore.collection("auth_tokens").document(tokenId)
                .set(authToken)
                .addOnSuccessListener(aVoid -> {
                    // Update user's refresh token
                    user.updateRefreshToken(tokenId);
                    
                    // Update user in Firestore
                    firestore.collection("users").document(user.getUserId())
                            .update(
                                "refreshToken", tokenId,
                                "tokenExpiry", user.getTokenExpiry(),
                                "lastLogin", user.getLastLogin()
                            );
                    
                    // Save credentials locally
                    saveCredentials(user.getUserId(), tokenId);
                    
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> 
                    callback.onError("Lỗi khi tạo phiên đăng nhập: " + e.getMessage())
                );
    }

    /**
     * Delete all user data from Firestore
     * @param userId User ID to delete
     * @param onComplete Callback when complete
     */
    private void deleteUserData(String userId, Runnable onComplete) {
        // Delete user's auth tokens
        firestore.collection("auth_tokens")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        doc.getReference().delete();
                    }
                    
                    // Delete user's albums
                    firestore.collection("albums")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(albumSnapshots -> {
                                for (DocumentSnapshot doc : albumSnapshots.getDocuments()) {
                                    doc.getReference().delete();
                                }
                                
                                // Delete user's favorite songs
                                firestore.collection("favorite_songs")
                                        .whereEqualTo("userId", userId)
                                        .get()
                                        .addOnSuccessListener(favoriteSnapshots -> {
                                            for (DocumentSnapshot doc : favoriteSnapshots.getDocuments()) {
                                                doc.getReference().delete();
                                            }
                                            
                                            // Delete user's listening history
                                            firestore.collection("listening_history")
                                                    .whereEqualTo("userId", userId)
                                                    .get()
                                                    .addOnSuccessListener(historySnapshots -> {
                                                        for (DocumentSnapshot doc : historySnapshots.getDocuments()) {
                                                            doc.getReference().delete();
                                                        }
                                                        
                                                        // Delete user's settings
                                                        firestore.collection("user_settings")
                                                                .document(userId)
                                                                .delete();
                                                        
                                                        // Finally delete user document
                                                        firestore.collection("users")
                                                                .document(userId)
                                                                .delete()
                                                                .addOnSuccessListener(aVoid -> onComplete.run())
                                                                .addOnFailureListener(e -> onComplete.run());
                                                    });
                                        });
                            });
                });
    }

    /**
     * Save user credentials to SharedPreferences
     * @param userId User ID
     * @param token Authentication token
     */
    private void saveCredentials(String userId, String token) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.apply();
    }

    /**
     * Clear all saved credentials and auto-login settings
     */
    public void clearSavedCredentials() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_AUTH_TOKEN);
        editor.remove(KEY_AUTO_LOGIN);
        editor.apply();

        // Delete auth token from Firestore if it exists
        String token = sharedPreferences.getString(KEY_AUTH_TOKEN, null);
        if (token != null) {
            firestore.collection("auth_tokens").document(token).delete();
        }

        // Sign out from Firebase
        firebaseAuth.signOut();
    }

    /**
     * Get device information
     * @return String containing device info
     */
    private String getDeviceInfo() {
        return "Android " + Build.VERSION.RELEASE + " (" + Build.MODEL + ")";
    }


    public void validatePassword(User user, String password, AuthCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = auth.getCurrentUser();

        if (firebaseUser == null || user == null || user.getEmail() == null) {
            callback.onError("Người dùng không hợp lệ.");
            return;
        }

        // Re-authenticate user before sensitive operation
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
        firebaseUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    callback.onSuccess(user);
                })
                .addOnFailureListener(e -> {
                    callback.onError("Mật khẩu không đúng hoặc lỗi xác thực: " + e.getMessage());
                });
    }
}
