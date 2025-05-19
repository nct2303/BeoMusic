package com.example.beomusic.adapters;

import com.example.beomusic.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beomusic.R;
import com.example.beomusic.models.Album;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for displaying albums in a RecyclerView
 */
public class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {

    private List<Album> albums;
    private final AlbumClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    /**
     * Interface for album click events
     */
    public interface AlbumClickListener {
        void onAlbumClick(Album album);
    }

    /**
     * Constructor
     * @param albums List of albums to display
     * @param listener Click listener
     */
    public AlbumAdapter(List<Album> albums, AlbumClickListener listener) {
        this.albums = albums;
        this.listener = listener;
    }

    /**
     * Update the list of albums
     * @param newAlbums New list of albums
     */
    public void updateAlbums(List<Album> newAlbums) {
        this.albums = newAlbums;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        return new AlbumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlbumViewHolder holder, int position) {
        Album album = albums.get(position);
        holder.bind(album);
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    /**
     * ViewHolder for album items
     */
    class AlbumViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAlbumCover;
        private final TextView tvAlbumTitle;
        private final TextView tvSongCount;
        private final TextView tvCreatedDate;

        public AlbumViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumCover = itemView.findViewById(R.id.ivAlbumCover);
            tvAlbumTitle = itemView.findViewById(R.id.tvAlbumTitle);
            tvSongCount = itemView.findViewById(R.id.tvSongCount);
            tvCreatedDate = itemView.findViewById(R.id.tvCreatedDate);

            // Set click listener
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onAlbumClick(albums.get(position));
                }
            });
        }

        /**
         * Bind album data to views
         * @param album Album to bind
         */
        public void bind(Album album) {
            tvAlbumTitle.setText(album.getTitle());
            tvSongCount.setText(itemView.getContext().getString(R.string.song_count, album.getSongCount()));
            
            if (album.getCreatedDate() != null) {
                tvCreatedDate.setText(dateFormat.format(album.getCreatedDate()));
            } else {
                tvCreatedDate.setText("");
            }

            // Load album cover image
            if (album.getCoverImageUrl() != null && !album.getCoverImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(album.getCoverImageUrl())
                        .placeholder(R.drawable.ic_album_placeholder)
                        .error(R.drawable.ic_album_placeholder)
                        .centerCrop()
                        .into(ivAlbumCover);
            } else {
                ivAlbumCover.setImageResource(R.drawable.ic_album_placeholder);
            }
        }
    }
}
