package com.example.beomusic.views;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.beomusic.R;
import com.example.beomusic.adapters.CommentAdapter;
import com.example.beomusic.models.Comment;
import com.example.beomusic.repositories.CommentRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Date;
import java.util.List;

public class ShowComment extends AppCompatActivity {

    private static final String TAG = "ShowComment";
    private static final int COMMENTS_PER_PAGE = 20;

    // UI Elements
    private Toolbar toolbar;
    private RecyclerView rvComments;
    private SwipeRefreshLayout swipeRefreshLayout;
    private EditText etCommentInput;
    private ImageButton btnSendComment;
    private ImageView ivCurrentUserAvatar;
    private ProgressBar progressBar;
    private TextView tvEmptyComments;

    // Adapters & Data
    private CommentAdapter commentAdapter;
    private CommentRepository commentRepository;
    private String songId;
    private String songTitle;
    private boolean isLoading = false;
    private Date lastCommentTimestamp = null;
    private boolean hasMoreComments = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_comment);
        
        Log.d(TAG, "onCreate: Starting comment activity");
        
        // Get intent data
        Intent intent = getIntent();
        songId = intent.getStringExtra("songId");
        songTitle = intent.getStringExtra("songTitle");

        Log.d(TAG, "onCreate: Song ID = " + songId + ", Title = " + songTitle);

        if (songId == null) {
            Log.e(TAG, "onCreate: Song ID is null, cannot load comments");
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin bài hát", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Initialize UI
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        
        // Load comments
        loadComments();
        
        // Load current user avatar
        loadCurrentUserAvatar();
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews: Initializing UI elements");
        toolbar = findViewById(R.id.toolbar);
        rvComments = findViewById(R.id.rvComments);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etCommentInput = findViewById(R.id.etCommentInput);
        btnSendComment = findViewById(R.id.btnSendComment);
        ivCurrentUserAvatar = findViewById(R.id.ivCurrentUserAvatar);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyComments = findViewById(R.id.tvEmptyComments);
        
        // Initialize repository
        commentRepository = new CommentRepository(this);
        Log.d(TAG, "initializeViews: CommentRepository initialized");
    }

    private void setupToolbar() {
        Log.d(TAG, "setupToolbar: Setting up toolbar");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title = "Bình luận" + (songTitle != null ? " - " + songTitle : "");
            getSupportActionBar().setTitle(title);
            Log.d(TAG, "setupToolbar: Toolbar title set to: " + title);
        } else {
            Log.w(TAG, "setupToolbar: getSupportActionBar() returned null");
        }
    }

    private void setupRecyclerView() {
        Log.d(TAG, "setupRecyclerView: Setting up RecyclerView");
        commentAdapter = new CommentAdapter(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(layoutManager);
        rvComments.setAdapter(commentAdapter);
        Log.d(TAG, "setupRecyclerView: RecyclerView setup complete");
        
        // Pagination listener
        rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    
                    if (!isLoading && hasMoreComments && totalItemCount <= (lastVisibleItem + 5)) {
                        // Load more comments when user is near the end of the list
                        Log.d(TAG, "onScrolled: Near end of list, loading more comments");
                        loadMoreComments();
                    }
                }
            }
        });
    }

    private void setupListeners() {
        Log.d(TAG, "setupListeners: Setting up button listeners");
        // Swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::refreshComments);
        
        // Send comment button
        btnSendComment.setOnClickListener(v -> {
            String content = etCommentInput.getText().toString().trim();
            if (!content.isEmpty()) {
                Log.d(TAG, "Send button clicked with content: " + content);
                postComment(content);
            } else {
                Log.w(TAG, "Send button clicked with empty content");
                Toast.makeText(this, "Vui lòng nhập nội dung bình luận", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadComments() {
        if (isLoading) {
            Log.d(TAG, "loadComments: Already loading, skipping");
            return;
        }
        
        Log.d(TAG, "loadComments: Loading comments for song ID: " + songId);
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyComments.setVisibility(View.GONE);
        
        commentRepository.getComments(songId, COMMENTS_PER_PAGE, new CommentRepository.CommentCallback() {
            @Override
            public void onSuccess(List<Comment> comments) {
                Log.d(TAG, "loadComments.onSuccess: Received " + comments.size() + " comments");
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    
                    commentAdapter.setComments(comments);
                    
                    if (comments.isEmpty()) {
                        Log.d(TAG, "loadComments.onSuccess: No comments found");
                        tvEmptyComments.setVisibility(View.VISIBLE);
                        hasMoreComments = false;
                    } else {
                        tvEmptyComments.setVisibility(View.GONE);
                        // Store the timestamp of the last comment for pagination
                        lastCommentTimestamp = comments.get(comments.size() - 1).getTimestamp();
                        hasMoreComments = comments.size() == COMMENTS_PER_PAGE;
                        Log.d(TAG, "loadComments.onSuccess: Last comment timestamp: " + lastCommentTimestamp);
                        Log.d(TAG, "loadComments.onSuccess: hasMoreComments = " + hasMoreComments);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "loadComments.onError: " + errorMessage);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    Toast.makeText(ShowComment.this, errorMessage, Toast.LENGTH_SHORT).show();
                    tvEmptyComments.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void loadMoreComments() {
        if (isLoading || !hasMoreComments || lastCommentTimestamp == null) {
            Log.d(TAG, "loadMoreComments: Skipping. isLoading=" + isLoading + ", hasMoreComments=" + hasMoreComments + 
                  ", lastCommentTimestamp=" + (lastCommentTimestamp != null ? "not null" : "null"));
            return;
        }
        
        Log.d(TAG, "loadMoreComments: Loading more comments starting after: " + lastCommentTimestamp);
        isLoading = true;
        
        commentRepository.getMoreComments(songId, lastCommentTimestamp, COMMENTS_PER_PAGE, new CommentRepository.CommentCallback() {
            @Override
            public void onSuccess(List<Comment> comments) {
                Log.d(TAG, "loadMoreComments.onSuccess: Received " + comments.size() + " more comments");
                runOnUiThread(() -> {
                    isLoading = false;
                    
                    if (comments.isEmpty()) {
                        // No more comments to load
                        Log.d(TAG, "loadMoreComments.onSuccess: No more comments available");
                        hasMoreComments = false;
                    } else {
                        commentAdapter.addComments(comments);
                        // Update last comment timestamp for next pagination
                        lastCommentTimestamp = comments.get(comments.size() - 1).getTimestamp();
                        hasMoreComments = comments.size() == COMMENTS_PER_PAGE;
                        Log.d(TAG, "loadMoreComments.onSuccess: Updated last comment timestamp: " + lastCommentTimestamp);
                        Log.d(TAG, "loadMoreComments.onSuccess: hasMoreComments = " + hasMoreComments);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "loadMoreComments.onError: " + errorMessage);
                runOnUiThread(() -> {
                    isLoading = false;
                    Toast.makeText(ShowComment.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void refreshComments() {
        Log.d(TAG, "refreshComments: Refreshing comments list");
        // Reset pagination state
        lastCommentTimestamp = null;
        hasMoreComments = true;
        
        // Load comments from the beginning
        loadComments();
    }

    private void postComment(String content) {
        // Check if user is logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w(TAG, "postComment: User is not logged in");
            Toast.makeText(this, "Bạn cần đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "postComment: Posting comment for user: " + currentUser.getUid() + ", song: " + songId);
        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        btnSendComment.setEnabled(false);
        
        // Add comment to database
        commentRepository.addComment(songId, content, new CommentRepository.SingleCommentCallback() {
            @Override
            public void onSuccess(Comment comment) {
                Log.d(TAG, "postComment.onSuccess: Comment added successfully with ID: " + comment.getCommentId());
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSendComment.setEnabled(true);
                    
                    // Clear input
                    etCommentInput.setText("");
                    
                    // Add comment to adapter
                    commentAdapter.addComment(comment);
                    
                    // Scroll to top to see the new comment
                    rvComments.smoothScrollToPosition(0);
                    
                    // Hide empty view if visible
                    tvEmptyComments.setVisibility(View.GONE);
                });
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "postComment.onError: " + errorMessage);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSendComment.setEnabled(true);
                    Toast.makeText(ShowComment.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void loadCurrentUserAvatar() {
        Log.d(TAG, "loadCurrentUserAvatar: Loading current user avatar");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getPhotoUrl() != null) {
            Log.d(TAG, "loadCurrentUserAvatar: User photo URL: " + currentUser.getPhotoUrl());
            Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.default_avatar)
                    .circleCrop()
                    .into(ivCurrentUserAvatar);
        } else {
            Log.d(TAG, "loadCurrentUserAvatar: No user photo available, using default");
            ivCurrentUserAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "onSupportNavigateUp: Navigating back");
        onBackPressed();
        return true;
    }
}