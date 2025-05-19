package com.example.beomusic.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.beomusic.R;
import com.example.beomusic.models.Song;

import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private Context context;
    private OnSongClickListener listener;
    private boolean isAlbumMode = false; // Chế độ Album để hiển thị nút xóa nhanh

    public interface OnSongClickListener {
        void onSongClick(Song song);
        void onMoreClick(Song song, View view);
        // Thêm phương thức cho nút xóa nhanh
        default void onRemoveClick(Song song) { /* Do nothing by default */ }
    }

    public SongAdapter(Context context, OnSongClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    // Setter cho chế độ Album
    public void setAlbumMode(boolean albumMode) {
        this.isAlbumMode = albumMode;
        notifyDataSetChanged();
    }

    public void setSongs(List<Song> songs) {
        this.songs = songs;
        notifyDataSetChanged();
    }
    
    public List<Song> getSongs(){
        return songs;
    }
    
    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.bind(song);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSong;
        TextView tvSongName;
        TextView tvArtistName;
        ImageButton btnMore;
        ImageButton btnRemove;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSong = itemView.findViewById(R.id.imgSong);
            tvSongName = itemView.findViewById(R.id.tvSongName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            btnMore = itemView.findViewById(R.id.btnMore);
            btnRemove = itemView.findViewById(R.id.btnRemove); // Nút xóa nhanh (thêm trong layout)

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSongClick(songs.get(position));
                }
            });

            btnMore.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMoreClick(songs.get(position), v);
                }
            });
            
            // Nút xóa nhanh - sẽ hiển thị khi ở chế độ Album
            if (btnRemove != null) {
                btnRemove.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null) {
                        listener.onRemoveClick(songs.get(position));
                    }
                });
            }
        }

        void bind(Song song) {
            tvSongName.setText(song.getTitle());
            tvArtistName.setText(song.getArtist());

            // Hiển thị hoặc ẩn nút xóa tùy theo chế độ
            if (btnRemove != null) {
                btnRemove.setVisibility(isAlbumMode ? View.VISIBLE : View.GONE);
            }

            // Sử dụng Glide để load ảnh
            Glide.with(context)
                    .load(song.getThumbnailUrl())
                    .into(imgSong);
        }
    }
}