package com.johendry.wisatator;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PenginapanAdapter extends RecyclerView.Adapter<PenginapanAdapter.ViewHolder> {

    private final Context context;
    private final List<Penginapan> list;

    public PenginapanAdapter(Context context, List<Penginapan> list) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_penginapan, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        Penginapan p = list.get(position);

        holder.tvNama.setText(p.getNama() != null ? p.getNama() : "-");
        holder.tvAlamat.setText(p.getAlamat() != null ? p.getAlamat() : "-");
        holder.tvHarga.setText("Rp " + p.getHargaPerMalam() + " / malam");

        // Load gambar (Google Drive atau resource lokal)
        if (!TextUtils.isEmpty(p.getImageUrl())) {
            String url = normalizeDriveUrl(p.getImageUrl());
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.ivGambar);
        } else if (p.getGambarRes() != null) {
            Glide.with(context)
                    .load(p.getGambarRes())
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.ivGambar);
        } else {
            holder.ivGambar.setImageResource(R.drawable.ic_launcher_background);
        }

        // Cek rating aman
        float rating = 0f;
        try {
            if (p.getRating() != null) {
                rating = p.getRating().floatValue();
            }
        } catch (Exception ignored) {}
        holder.ratingBar.setRating(rating);

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, DetailActivity.class);
            i.putExtra("penginapan", p);
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // 🔹 Tambahkan agar bisa update list dari luar
    public void updateList(List<Penginapan> newList) {
        list.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGambar;
        TextView tvNama, tvAlamat, tvHarga;
        RatingBar ratingBar;

        public ViewHolder(@NotNull View itemView) {
            super(itemView);
            ivGambar = itemView.findViewById(R.id.ivPenginapan);
            tvNama = itemView.findViewById(R.id.tvNamaPenginapan);
            tvAlamat = itemView.findViewById(R.id.tvAlamatPenginapan);
            tvHarga = itemView.findViewById(R.id.tvHargaPenginapan);
            ratingBar = itemView.findViewById(R.id.ratingBarPenginapan);
        }
    }

    // 🔹 Convert Google Drive link ke direct link
    private String normalizeDriveUrl(String input) {
        if (input == null) return null;
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
