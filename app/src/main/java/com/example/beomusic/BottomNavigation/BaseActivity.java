package com.example.beomusic.BottomNavigation;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.beomusic.R;
import com.example.beomusic.views.HomeActivity;
import com.example.beomusic.views.album.AlbumListActivity;
import com.example.beomusic.views.album.SongDetailActivity;
import com.example.beomusic.views.auth.AccountActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
        initBottomNavigation();
    }

    // Phương thức abstract để mỗi Activity con cung cấp layout riêng
    protected abstract int getLayoutResourceId();

    // Phương thức để lấy ID menu item mặc định của activity
    protected abstract int getDefaultNavigationItemId();

    private void initBottomNavigation() {
        bottomNavigationView = findViewById(R.id.studentBottomNavigation);
        if (bottomNavigationView != null) {
            setupBottomNavigation(getDefaultNavigationItemId());
        }
    }

    protected void setupBottomNavigation(int selectedItemId) {
        // Thiết lập item được chọn
        bottomNavigationView.setSelectedItemId(selectedItemId);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Tránh chuyển đến activity hiện tại
            if (itemId == selectedItemId) {
                return true;
            }

            Intent intent = null;

            if (itemId == R.id.nav_home) {
                intent = new Intent(BaseActivity.this, HomeActivity.class);
            } else if (itemId == R.id.nav_album) {
                intent = new Intent(BaseActivity.this, AlbumListActivity.class);
            } else if (itemId == R.id.nav_profile) {
                intent = new Intent(BaseActivity.this, AccountActivity.class);
            }

            if (intent != null) {
                // Sử dụng FLAG_ACTIVITY_CLEAR_TOP để tránh tạo nhiều instance
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }


}