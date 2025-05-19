package com.example.beomusic.ultis;

import com.example.beomusic.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.annotation.NonNull;

public class UserUtils {

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public static void fetchUserInfo(String userId, final UserCallback callback) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            User user = task.getResult().toObject(User.class);
                            if (user != null) {
                                callback.onSuccess(user);
                            } else {
                                callback.onFailure("User data is null");
                            }
                        } else {
                            callback.onFailure(task.getException() != null ? task.getException().getMessage() : "Failed to fetch user data");
                        }
                    }
                });
    }
} 