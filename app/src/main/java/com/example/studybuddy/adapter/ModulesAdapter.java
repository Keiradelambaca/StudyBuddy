package com.example.studybuddy.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.studybuddy.R;

import java.util.List;

public class ModulesAdapter extends RecyclerView.Adapter<ModulesAdapter.ModuleViewHolder> {

    public interface OnModuleClickListener {
        void onModuleClick(Module module);
    }

    private final List<Module> modules;
    private final OnModuleClickListener listener;

    public ModulesAdapter(List<Module> modules, OnModuleClickListener listener) {
        this.modules = modules;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ModuleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_module_card, parent, false);
        return new ModuleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModuleViewHolder holder, int position) {
        Module module = modules.get(position);

        holder.tvTitle.setText(module.getTitle());

        // Subtitle preference: description if exists, otherwise Year/Semester
        String desc = module.getDescription() == null ? "" : module.getDescription().trim();
        if (!desc.isEmpty()) {
            holder.tvSubtitle.setText(desc);
        } else {
            holder.tvSubtitle.setText(module.getMetaText());
        }

        holder.tvMeta.setText(module.getMetaText());

        View clickable = holder.itemView.findViewById(R.id.moduleRowRoot); // add id in xml (below)
        if (clickable == null) clickable = holder.itemView;

        clickable.setOnClickListener(v -> {
            if (listener != null) listener.onModuleClick(module);
        });

    }

    @Override
    public int getItemCount() {
        return modules == null ? 0 : modules.size();
    }

    static class ModuleViewHolder extends RecyclerView.ViewHolder {

        TextView tvTitle, tvSubtitle, tvMeta;
        ImageView ivChevron;

        ModuleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvModuleTitle);
            tvSubtitle = itemView.findViewById(R.id.tvModuleSubtitle);
            tvMeta = itemView.findViewById(R.id.tvModuleMeta);
        }
    }
}
