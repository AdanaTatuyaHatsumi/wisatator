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

public class KulinerAdapter extends RecyclerView.Adapter<KulinerAdapter.ViewHolder> {

    private final Context context;
    private final List<Kuliner> list;

    public KulinerAdapter(Context context, List<Kuliner> list) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_kuliner, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        Kuliner k = list.get(position);

        holder.tvNama.setText(k.getNama() != null ? k.getNama() : "-");
        holder.tvLokasi.setText(k.getLokasi() != null ? k.getLokasi() : "-");
        holder.tvDeskripsi.setText(k.getDeskripsi() != null ? k.getDeskripsi() : "-");

        // Load gambar (Google Drive atau resource lokal)
        if (!TextUtils.isEmpty(k.getImageUrl())) {
            String url = normalizeDriveUrl(k.getImageUrl());
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.ivGambar);
        } else if (k.getGambarRes() != 0) {
            Glide.with(context)
                    .load(k.getGambarRes())
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.ivGambar);
        } else {
            holder.ivGambar.setImageResource(R.drawable.ic_launcher_background);
        }

        // Cek rating aman
        float rating = 0f;
        try {
            if (k.getRating() != null) {
                rating = k.getRating().floatValue();
            }
        } catch (Exception ignored) {}
        holder.ratingBar.setRating(rating);

        holder.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, DetailActivity.class);
            i.putExtra("kuliner", k);

            // 🔥 TAMBAHKAN INI
            i.putExtra(DetailActivity.EXTRA_COLLECTION, "kuliner");
            i.putExtra(DetailActivity.EXTRA_DOCID, String.valueOf(k.getId()));

            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // 🔹 Tambahkan agar bisa update list dari luar
    public void updateList(List<Kuliner> newList) {
        list.clear();
        if (newList != null) {
            list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGambar;
        TextView tvNama, tvLokasi, tvDeskripsi;
        RatingBar ratingBar;

        public ViewHolder(@NotNull View itemView) {
            super(itemView);
            ivGambar = itemView.findViewById(R.id.ivKuliner);
            tvNama = itemView.findViewById(R.id.tvNamaKuliner);
            tvLokasi = itemView.findViewById(R.id.tvLokasiKuliner);
            tvDeskripsi = itemView.findViewById(R.id.tvDeskripsiKuliner);
            ratingBar = itemView.findViewById(R.id.ratingBarKuliner);
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
