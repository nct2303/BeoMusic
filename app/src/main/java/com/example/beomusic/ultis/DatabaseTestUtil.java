package com.example.beomusic.ultis;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

/**
 * Utility class for testing database connections
 */
public class DatabaseTestUtil {
    
    private static final String TAG = "DatabaseTestUtil";
    
    /**
     * Interface for database connection test callback
     */
    public interface DatabaseTestCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }
    
    /**
     * Test connection to Firestore database
     * @param callback Callback for test result
     */
    public static void testFirestoreConnection(DatabaseTestCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Try to get a small collection to test connection
        db.collection("users")
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firestore connection test successful");
                            callback.onSuccess("Kết nối đến Firestore thành công");
                        } else {
                            Log.e(TAG, "Firestore connection test failed", task.getException());
                            callback.onError("Kết nối đến Firestore thất bại: " + 
                                    (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"));
                        }
                    }
                });
    }
    
    /**
     * Test connection to Firebase Authentication
     * @param callback Callback for test result
     */
    public static void testAuthConnection(DatabaseTestCallback callback) {
        // Firebase Auth is initialized when the app starts
        // We can check if it's properly initialized by checking the current user (even if null)
        try {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            // Just getting the current user (even if null) without error means the connection is working
            auth.getCurrentUser();
            Log.d(TAG, "Firebase Auth connection test successful");
            callback.onSuccess("Kết nối đến Firebase Authentication thành công");
        } catch (Exception e) {
            Log.e(TAG, "Firebase Auth connection test failed", e);
            callback.onError("Kết nối đến Firebase Authentication thất bại: " + e.getMessage());
        }
    }
    
    /**
     * Test all database connections
     * @param callback Callback for test result
     */
    public static void testAllConnections(DatabaseTestCallback callback) {
        final StringBuilder resultBuilder = new StringBuilder();
        final boolean[] firestoreSuccess = {false};
        final boolean[] authSuccess = {false};

        // Định nghĩa biến Runnable để kiểm tra kết quả
        final Runnable checkAllComplete = new Runnable() {
            @Override
            public void run() {
                // Kiểm tra xem kết quả có chứa thông tin từ cả Firestore và Firebase Authentication hay không
                if (resultBuilder.toString().contains("Firestore") &&
                        resultBuilder.toString().contains("Firebase Authentication")) {
                    if (firestoreSuccess[0] && authSuccess[0]) {
                        callback.onSuccess("Tất cả kết nối thành công:\n" + resultBuilder.toString());
                    } else {
                        callback.onError("Một số kết nối thất bại:\n" + resultBuilder.toString());
                    }
                }
            }
        };

        // Test Firestore
        testFirestoreConnection(new DatabaseTestCallback() {
            @Override
            public void onSuccess(String message) {
                firestoreSuccess[0] = true;
                resultBuilder.append(message).append("\n");
                checkAllComplete.run();
            }

            @Override
            public void onError(String errorMessage) {
                resultBuilder.append(errorMessage).append("\n");
                checkAllComplete.run();
            }
        });

        // Test Firebase Authentication
        testAuthConnection(new DatabaseTestCallback() {
            @Override
            public void onSuccess(String message) {
                authSuccess[0] = true;
                resultBuilder.append(message).append("\n");
                checkAllComplete.run();
            }

            @Override
            public void onError(String errorMessage) {
                resultBuilder.append(errorMessage).append("\n");
                checkAllComplete.run();
            }
        });
    }
}
