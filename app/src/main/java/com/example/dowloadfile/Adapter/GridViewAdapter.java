package com.example.dowloadfile.Adapter;

import com.example.dowloadfile.R;
import com.example.dowloadfile.Model.DownloadModel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class GridViewAdapter extends BaseAdapter {
    Context context;
    ArrayList<DownloadModel> arrayList;
    OnItemClickListener onItemClickListener;

    public GridViewAdapter(Context context, ArrayList<DownloadModel> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @Override
    public int getCount() {
        return arrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return arrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_grid_firebase, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(arrayList.get(position).getTitle());
        Glide.with(context).load(arrayList.get(position).getFile_path()).into(holder.imageView);

        convertView.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                onItemClickListener.onClick(arrayList.get(position));
            }
        });

        return convertView;
    }

    public static class ViewHolder {
        ImageView imageView;
        TextView title;

        public ViewHolder(@NonNull View itemView) {
            title = itemView.findViewById(R.id.gridCaption);
            imageView = itemView.findViewById(R.id.gridImage);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClick(DownloadModel image);
    }
}

