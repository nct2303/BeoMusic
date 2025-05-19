package com.example.beomusic.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beomusic.R;
import com.example.beomusic.models.Comment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private static final String TAG = "CommentAdapter";
    private Context context;
    private List<Comment> commentList;
    private SimpleDateFormat dateFormat;

    public CommentAdapter(Context context) {
        this.context = context;
        this.commentList = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        Log.d(TAG, "CommentAdapter: Initialized with empty comment list");
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder: Creating new view holder");
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        Log.d(TAG, "onBindViewHolder: Binding comment at position " + position + ", ID: " + 
              (comment.getCommentId() != null ? comment.getCommentId() : "null"));
        
        // Set username
        String username = comment.getUsername();
        holder.tvUsername.setText(username != null ? username : "Anonymous");
        
        // Set content
        String content = comment.getContent();
        if (content != null && !content.isEmpty()) {
            holder.tvContent.setText(content);
        } else {
            Log.w(TAG, "onBindViewHolder: Comment content is empty for comment ID: " + comment.getCommentId());
            holder.tvContent.setText("[No content]");
        }
        
        // Format and display timestamp
        Date timestamp = comment.getTimestamp();
        if (timestamp != null) {
            try {
                String formattedDate = dateFormat.format(timestamp);
                holder.tvTimestamp.setText(formattedDate);
                Log.d(TAG, "onBindViewHolder: Timestamp formatted: " + formattedDate);
            } catch (Exception e) {
                Log.e(TAG, "onBindViewHolder: Error formatting timestamp", e);
                holder.tvTimestamp.setText("Unknown date");
            }
        } else {
            Log.w(TAG, "onBindViewHolder: Timestamp is null for comment ID: " + comment.getCommentId());
            holder.tvTimestamp.setText("Vừa xong");
        }
        
        // Load user avatar
        try {
            String photoUrl = comment.getUserPhotoUrl();
            if (photoUrl != null && !photoUrl.isEmpty()) {
                Log.d(TAG, "onBindViewHolder: Loading user photo from URL: " + photoUrl);
                Glide.with(context)
                        .load(photoUrl)
                        .placeholder(R.drawable.default_avatar)
                        .circleCrop()
                        .into(holder.ivUserAvatar);
            } else {
                Log.d(TAG, "onBindViewHolder: No photo URL, using default avatar");
                holder.ivUserAvatar.setImageResource(R.drawable.default_avatar);
            }
        } catch (Exception e) {
            Log.e(TAG, "onBindViewHolder: Error loading user avatar", e);
            holder.ivUserAvatar.setImageResource(R.drawable.default_avatar);
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public void setComments(List<Comment> comments) {
        Log.d(TAG, "setComments: Setting " + comments.size() + " comments");
        this.commentList = comments;
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        Log.d(TAG, "addComment: Adding new comment at position 0, ID: " + 
              (comment.getCommentId() != null ? comment.getCommentId() : "null"));
        this.commentList.add(0, comment); // Add to the top
        notifyItemInserted(0);
    }

    public void addComments(List<Comment> comments) {
        Log.d(TAG, "addComments: Adding " + comments.size() + " more comments");
        int oldSize = this.commentList.size();
        this.commentList.addAll(comments);
        notifyItemRangeInserted(oldSize, comments.size());
    }

    public void clear() {
        Log.d(TAG, "clear: Clearing all comments");
        this.commentList.clear();
        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUsername;
        TextView tvContent;
        TextView tvTimestamp;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
} 