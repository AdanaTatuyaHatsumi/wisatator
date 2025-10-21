package com.johendry.wisatator;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class FullscreenGalleryActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGES = "extra_images";
    public static final String EXTRA_POS = "extra_pos";

    private ViewPager2 viewPager;
    private ImageButton btnClose;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_gallery);

        viewPager = findViewById(R.id.viewPager);
        btnClose = findViewById(R.id.btnClose);

        ArrayList<String> images = getIntent().getStringArrayListExtra(EXTRA_IMAGES);
        int pos = getIntent().getIntExtra(EXTRA_POS, 0);

        if (images == null) images = new ArrayList<>();

        FullscreenPagerAdapter adapter = new FullscreenPagerAdapter(images);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(Math.max(0, pos), false);

        btnClose.setOnClickListener(v -> finish());
    }
}
