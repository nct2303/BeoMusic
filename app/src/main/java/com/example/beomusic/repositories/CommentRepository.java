package com.example.beomusic.repositories;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;

import com.example.beomusic.models.Comment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentRepository {
    
    private static final String TAG = "CommentRepository";
    private static final String COMMENTS_COLLECTION = "comments";
    private final FirebaseFirestore firestore;
    private final FirebaseAuth firebaseAuth;
    private final Context context;
    
    public interface CommentCallback {
        void onSuccess(List<Comment> comments);
        void onError(String errorMessage);
    }
    
    public interface SingleCommentCallback {
        void onSuccess(Comment comment);
        void onError(String errorMessage);
    }
    
    public CommentRepository(Context context) {
        this.context = context;
        this.firestore = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
        Log.d(TAG, "CommentRepository initialized");
    }
    
    /**
     * Get comments for a specific song
     * @param songId The ID of the song
     * @param limit Maximum number of comments to fetch
     * @param callback Callback for result
     */
    public void getComments(String songId, int limit, CommentCallback callback) {
        Log.d(TAG, "getComments: Fetching comments for song ID: " + songId + ", limit: " + limit);
        
        // Using only whereEqualTo without orderBy to avoid requiring a composite index
        firestore.collection(COMMENTS_COLLECTION)
                .whereEqualTo("songId", songId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> comments = new ArrayList<>();
                    Log.d(TAG, "getComments: Query returned " + queryDocumentSnapshots.size() + " documents");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d(TAG, "getComments: Processing document ID: " + document.getId());
                        try {
                            Comment comment = document.toObject(Comment.class);
                            if (comment != null) {
                                comments.add(comment);
                                Log.d(TAG, "getComments: Added comment with ID: " + comment.getCommentId() + 
                                          ", by user: " + comment.getUserId() + 
                                          ", content: " + (comment.getContent() != null ? comment.getContent().substring(0, Math.min(20, comment.getContent().length())) + "..." : "null"));
                            } else {
                                Log.w(TAG, "getComments: Failed to convert document to Comment: " + document.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "getComments: Error converting document to Comment: " + document.getId(), e);
                        }
                    }
                    
                    // Sort comments by timestamp locally
                    comments.sort((c1, c2) -> {
                        if (c1.getTimestamp() == null) return 1;
                        if (c2.getTimestamp() == null) return -1;
                        return c2.getTimestamp().compareTo(c1.getTimestamp()); // Descending order
                    });
                    
                    // Apply limit after sorting
                    if (comments.size() > limit) {
                        comments = comments.subList(0, limit);
                    }
                    
                    Log.d(TAG, "getComments: Successfully converted " + comments.size() + " comments");
                    callback.onSuccess(comments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getComments: Error fetching comments for song ID: " + songId, e);
                    callback.onError("Lỗi khi tải bình luận: " + e.getMessage());
                });
    }
    
    /**
     * Get more comments for pagination
     * @param songId The ID of the song
     * @param lastTimestamp Timestamp of the last loaded comment
     * @param limit Maximum number of comments to fetch
     * @param callback Callback for result
     */
    public void getMoreComments(String songId, Date lastTimestamp, int limit, CommentCallback callback) {
        Log.d(TAG, "getMoreComments: Fetching more comments for song ID: " + songId + 
              ", after timestamp: " + lastTimestamp + 
              ", limit: " + limit);
        
        // Using only whereEqualTo without orderBy to avoid requiring a composite index
        firestore.collection(COMMENTS_COLLECTION)
                .whereEqualTo("songId", songId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> allComments = new ArrayList<>();
                    List<Comment> filteredComments = new ArrayList<>();
                    Log.d(TAG, "getMoreComments: Query returned " + queryDocumentSnapshots.size() + " documents");
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Log.d(TAG, "getMoreComments: Processing document ID: " + document.getId());
                        try {
                            Comment comment = document.toObject(Comment.class);
                            if (comment != null) {
                                allComments.add(comment);
                            } else {
                                Log.w(TAG, "getMoreComments: Failed to convert document to Comment: " + document.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "getMoreComments: Error converting document to Comment: " + document.getId(), e);
                        }
                    }
                    
                    // Sort comments by timestamp
                    allComments.sort((c1, c2) -> {
                        if (c1.getTimestamp() == null) return 1;
                        if (c2.getTimestamp() == null) return -1;
                        return c2.getTimestamp().compareTo(c1.getTimestamp()); // Descending order
                    });
                    
                    // Find the index of the last comment
                    int startIndex = -1;
                    for (int i = 0; i < allComments.size(); i++) {
                        Comment comment = allComments.get(i);
                        if (comment.getTimestamp() != null && comment.getTimestamp().compareTo(lastTimestamp) < 0) {
                            startIndex = i;
                            break;
                        }
                    }
                    
                    // Get next batch of comments
                    if (startIndex != -1) {
                        int endIndex = Math.min(startIndex + limit, allComments.size());
                        filteredComments = allComments.subList(startIndex, endIndex);
                    }
                    
                    Log.d(TAG, "getMoreComments: Filtered to " + filteredComments.size() + " comments");
                    callback.onSuccess(filteredComments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "getMoreComments: Error fetching more comments for song ID: " + songId, e);
                    callback.onError("Lỗi khi tải thêm bình luận: " + e.getMessage());
                });
    }
    
    /**
     * Add a new comment to a song
     * @param songId The ID of the song
     * @param content Comment content
     * @param callback Callback for result
     */
    public void addComment(String songId, String content, SingleCommentCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "addComment: No user logged in");
            callback.onError("Bạn cần đăng nhập để bình luận");
            return;
        }
        
        Log.d(TAG, "addComment: Adding comment for song ID: " + songId + 
              ", user ID: " + currentUser.getUid() + 
              ", content: " + content.substring(0, Math.min(20, content.length())) + "...");
        
        // Get user data from Firestore
        firestore.collection("users").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String userPhotoUrl = documentSnapshot.getString("photoUrl");
                        
                        Log.d(TAG, "addComment: Retrieved user data. Username: " + username + 
                              ", has photo: " + (userPhotoUrl != null && !userPhotoUrl.isEmpty()));
                        
                        // Create comment object
                        Comment comment = new Comment();
                        comment.setUserId(currentUser.getUid());
                        comment.setSongId(songId);
                        comment.setContent(content);
                        comment.setUsername(username != null ? username : "Anonymous");
                        comment.setUserPhotoUrl(userPhotoUrl);
                        // Timestamp will be set by Firestore server
                        
                        Log.d(TAG, "addComment: Comment object created, saving to Firestore");
                        
                        // Save comment to Firestore
                        firestore.collection(COMMENTS_COLLECTION)
                                .add(comment)
                                .addOnSuccessListener(documentReference -> {
                                    String commentId = documentReference.getId();
                                    Log.d(TAG, "addComment: Comment saved successfully with ID: " + commentId);
                                    
                                    comment.setCommentId(commentId);
                                    callback.onSuccess(comment);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "addComment: Error saving comment to Firestore", e);
                                    callback.onError("Lỗi khi thêm bình luận: " + e.getMessage());
                                });
                    } else {
                        Log.w(TAG, "addComment: User document does not exist for user ID: " + currentUser.getUid());
                        callback.onError("Không tìm thấy thông tin người dùng");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "addComment: Error getting user data", e);
                    callback.onError("Lỗi khi tải thông tin người dùng: " + e.getMessage());
                });
    }
    
    /**
     * Delete a comment
     * @param commentId The ID of the comment to delete
     * @param callback Callback for result
     */
    public void deleteComment(String commentId, OnSuccessListener<Void> onSuccessListener, OnFailureListener onFailureListener) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "deleteComment: No user logged in");
            onFailureListener.onFailure(new Exception("Bạn cần đăng nhập để xóa bình luận"));
            return;
        }
        
        Log.d(TAG, "deleteComment: Attempting to delete comment ID: " + commentId + 
              " by user ID: " + currentUser.getUid());
        
        // Get the comment to check ownership
        firestore.collection(COMMENTS_COLLECTION).document(commentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Comment comment = documentSnapshot.toObject(Comment.class);
                    if (comment != null && comment.getUserId().equals(currentUser.getUid())) {
                        // User is the owner, proceed with deletion
                        Log.d(TAG, "deleteComment: User is owner, proceeding with deletion");
                        
                        firestore.collection(COMMENTS_COLLECTION).document(commentId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "deleteComment: Comment deleted successfully");
                                    onSuccessListener.onSuccess(aVoid);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "deleteComment: Error deleting comment", e);
                                    onFailureListener.onFailure(e);
                                });
                    } else {
                        Log.w(TAG, "deleteComment: User is not the owner of this comment");
                        onFailureListener.onFailure(new Exception("Bạn không có quyền xóa bình luận này"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "deleteComment: Error getting comment", e);
                    onFailureListener.onFailure(e);
                });
    }
} 