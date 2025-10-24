package com.johendry.wisatator;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.VH> {

    private final Context ctx;
    private final ArrayList<String> items;

    public GalleryAdapter(Context ctx, ArrayList<String> items) {
        this.ctx = ctx;
        this.items = items != null ? items : new ArrayList<>();
    }

    public void setItems(ArrayList<String> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_gallery_thumbnail, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String url = items.get(position);
        if (url == null) {
            holder.iv.setImageResource(R.drawable.ic_image_placeholder);
        } else {
            Glide.with(ctx)
                    .load(url)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(holder.iv);
        }

        // optional: buka full screen saat diklik (pakai Intent ke FullscreenActivity jika ada)
        holder.itemView.setOnClickListener(v -> {
            try {
                // contoh membuka di browser (atau buat FullscreenActivity)
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (i.resolveActivity(ctx.getPackageManager()) != null) {
                    ctx.startActivity(i);
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView iv;
        VH(@NonNull View v) {
            super(v);
            iv = v.findViewById(R.id.ivThumb);
        }
    }
}
