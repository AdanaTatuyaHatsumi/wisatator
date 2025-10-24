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

public class WisataAdapter extends RecyclerView.Adapter<WisataAdapter.ViewHolder> {

    private final Context context;
    private final List<Wisata> list;

    public WisataAdapter(@NotNull Context context, List<Wisata> list) {
        this.context = context;
        this.list = list != null ? list : new ArrayList<>();
    }

    @NotNull
    @Override
    public WisataAdapter.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wisata, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NotNull WisataAdapter.ViewHolder holder, int position) {
        if (list == null || position < 0 || position >= list.size()) return;
        Wisata w = list.get(position);

        holder.tvNama.setText(!TextUtils.isEmpty(w.getNama()) ? w.getNama() : "-");
        holder.tvLokasi.setText(!TextUtils.isEmpty(w.getLokasi()) ? w.getLokasi() : "-");

        // --- Deskripsi preview (2 lines) ---
        String desc = w.getDeskripsi();
        if (!TextUtils.isEmpty(desc)) {
            holder.tvDeskripsi.setText(desc);
            holder.tvDeskripsi.setVisibility(View.VISIBLE);
        } else {
            holder.tvDeskripsi.setText("");
            holder.tvDeskripsi.setVisibility(View.GONE);
        }

        // --- Gambar (Glide + normalisasi Google Drive) ---
        String url = w.getImageUrl();
        if (!TextUtils.isEmpty(url)) {
            url = normalizeDriveUrl(url);
            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_broken_image)
                    .fallback(R.drawable.ic_launcher_background)
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

            // 🔥 TAMBAHKAN INI
            i.putExtra(DetailActivity.EXTRA_COLLECTION, "wisata");
            i.putExtra(DetailActivity.EXTRA_DOCID, w.getId());

            if (!TextUtils.isEmpty(w.getImageUrl())) {
                i.putExtra("imageUrl", normalizeDriveUrl(w.getImageUrl()));
            }
            context.startActivity(i);
        });

        // === 🔥 Tambahan logika untuk besar-kecil ===
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        if (params != null) {
            if (position == 0) {
                // item pertama → lebih besar
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = 500; // px, bisa diganti dp convert
            } else {
                // item lain → lebih kecil
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = 250;
            }
            holder.itemView.setLayoutParams(params);
        }
    }


    @Override
    public int getItemCount() {
        return (list != null) ? list.size() : 0;
    }

    public void updateList(@NotNull List<Wisata> newList) {
        list.clear();
        if (newList != null) list.addAll(newList);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivGambar;
        TextView tvNama, tvLokasi, tvDeskripsi;

        public ViewHolder(@NotNull View itemView) {
            super(itemView);
            ivGambar = itemView.findViewById(R.id.ivWisata);
            tvNama = itemView.findViewById(R.id.tvNamaWisata);
            tvLokasi = itemView.findViewById(R.id.tvLokasiWisata);
            tvDeskripsi = itemView.findViewById(R.id.tvDeskripsiWisata); // baru
        }
    }

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
