package com.johendry.wisatator;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;

import java.util.ArrayList;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.VH> {

    private static final String TAG = "GalleryAdapter";
    private final Context ctx;
    private final ArrayList<String> items;

    public GalleryAdapter(Context ctx, ArrayList<String> items) {
        this.ctx = ctx;
        this.items = items != null ? items : new ArrayList<>();
        Log.d(TAG, "GalleryAdapter created initialSize=" + this.items.size());
    }

    public void setItems(@NonNull ArrayList<String> newItems) {
        Log.d(TAG, "setItems called size=" + (newItems != null ? newItems.size() : 0));
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
        Log.d(TAG, "items now size=" + items.size());
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
        Log.d(TAG, "onBindViewHolder pos=" + position + " url=" + url);
        holder.iv.setImageResource(R.drawable.ic_image_placeholder);

        if (url == null || url.trim().isEmpty()) {
            Log.w(TAG, "empty url pos=" + position);
            holder.iv.setImageResource(R.drawable.ic_image_placeholder);
            return;
        }

        Glide.with(ctx)
                .load(url)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.w(TAG, "Glide failed pos=" + position + " url=" + url + " err=" + (e != null ? e.getMessage() : "null"));
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d(TAG, "Glide ready pos=" + position + " url=" + url + " src=" + dataSource);
                        return false;
                    }
                })
                .into(holder.iv);

        holder.itemView.setOnClickListener(v -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                ctx.startActivity(i);
            } catch (Exception e) {
                Log.w(TAG, "open image intent failed", e);
            }
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
