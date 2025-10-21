package com.johendry.wisatator;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmallWisataAdapter extends RecyclerView.Adapter<SmallWisataAdapter.ViewHolder> {

    private final Context context;
    private final List<Wisata> list;

    public SmallWisataAdapter(Context context, List<Wisata> list) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
    }

    @NotNull
    @Override
    public SmallWisataAdapter.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wisata_small, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NotNull SmallWisataAdapter.ViewHolder holder, int position) {
        if (position < 0 || position >= list.size()) return;
        Wisata w = list.get(position);

        holder.tvNama.setText(!TextUtils.isEmpty(w.getNama()) ? w.getNama() : "-");
        holder.tvLokasi.setText(!TextUtils.isEmpty(w.getLokasi()) ? w.getLokasi() : "-");

        String desc = w.getDeskripsi();
        if (!TextUtils.isEmpty(desc)) {
            holder.tvDeskripsi.setText(desc);
            holder.tvDeskripsi.setVisibility(View.VISIBLE);
        } else {
            holder.tvDeskripsi.setVisibility(View.GONE);
        }

        String url = w.getImageUrl();
        if (!TextUtils.isEmpty(url)) url = normalizeDriveUrl(url);

        if (!TextUtils.isEmpty(url)) {
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .into(holder.ivGambar);
        } else if (w.getGambarRes() != 0) {
            Glide.with(context)
                    .load(w.getGambarRes())
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.ivGambar);
        } else {
            holder.ivGambar.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, DetailActivity.class);
            i.putExtra("wisata", w);
            if (!TextUtils.isEmpty(w.getImageUrl())) i.putExtra("imageUrl", normalizeDriveUrl(w.getImageUrl()));
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void updateList(List<Wisata> newList) {
        list.clear();
        if (newList != null) list.addAll(newList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGambar;
        TextView tvNama, tvLokasi, tvDeskripsi;

        ViewHolder(@NotNull View v) {
            super(v);
            ivGambar = v.findViewById(R.id.ivWisataSmall);
            tvNama = v.findViewById(R.id.tvNamaWisataSmall);
            tvLokasi = v.findViewById(R.id.tvLokasiWisataSmall);
            tvDeskripsi = v.findViewById(R.id.tvDeskripsiWisataSmall);
        }
    }

    // sama normalizeDriveUrl seperti sebelumnya
    private String normalizeDriveUrl(String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.contains("drive.google.com/uc?export=view")) return input;

        Pattern p = Pattern.compile("/d/([a-zA-Z0-9_-]+)");
        Matcher m = p.matcher(input);
        if (m.find()) return "https://drive.google.com/uc?export=view&id=" + m.group(1);

        p = Pattern.compile("[?&]id=([a-zA-Z0-9_-]+)");
        m = p.matcher(input);
        if (m.find()) return "https://drive.google.com/uc?export=view&id=" + m.group(1);

        p = Pattern.compile("open\\?id=([a-zA-Z0-9_-]+)");
        m = p.matcher(input);
        if (m.find()) return "https://drive.google.com/uc?export=view&id=" + m.group(1);

        return input;
    }
}
