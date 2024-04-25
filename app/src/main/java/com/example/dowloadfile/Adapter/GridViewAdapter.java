package com.example.dowloadfile.Adapter;

import com.example.dowloadfile.R;
import com.example.dowloadfile.Model.DownloadModel;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.dowloadfile.Utils.GridItemClickListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

public class GridViewAdapter extends BaseAdapter {
    Context context;
    private ActionMode mode;
    ArrayList<DownloadModel> arrayList;
    ArrayList<DownloadModel> selectList = new ArrayList<>();
    GridItemClickListener gridItemClickListener;
    SparseBooleanArray selectedItems;
    boolean isEnabled = false;
    boolean isSelectAll = false;
    LinearLayout linearLayout1, linearLayout2;

    public GridViewAdapter(Context context, ArrayList<DownloadModel> arrayList, GridItemClickListener listener, LinearLayout linearLayout1, LinearLayout linearLayout2) {
        this.context = context;
        this.arrayList = arrayList;
        this.gridItemClickListener = listener;
        this.selectedItems = new SparseBooleanArray();
        this.linearLayout1 = linearLayout1;
        this.linearLayout2 = linearLayout2;
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

        // Set background color based on selection state
        if (selectedItems.get(position)) {
            convertView.setBackgroundColor(Color.GRAY);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (gridItemClickListener != null) {
                    gridItemClickListener.onItemLongClick(position);
                }

                if (selectedItems.get(position)) {
                    selectedItems.delete(position);
                } else {
                    selectedItems.put(position, true);
                }
                notifyDataSetChanged();

                if(!isEnabled){
                    ActionMode.Callback callback = new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            MenuInflater menuInflater = mode.getMenuInflater();
                            menuInflater.inflate(R.menu.delete_menu, menu);
                            GridViewAdapter.this.mode = mode;
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            isEnabled = true;
                            ClickItem(position);
                            return false;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            int menuItem = item.getItemId();
                            if (menuItem == R.id.action_delete){
                                deleteSelectedFiles();
                                mode.finish();
                            }
                            if (menuItem == R.id.action_select_all){
                                if (selectList.size() == arrayList.size()){
                                    isSelectAll = false;
                                    selectList.clear();
                                    for (int i = 0; i < arrayList.size(); i++) {
                                        selectedItems.put(i, false);
                                    }
                                    mode.finish();
                                }
                                else {
                                    isSelectAll = true;
                                    selectList.clear();
                                    selectList.addAll(arrayList);
                                    for (int i = 0; i < arrayList.size(); i++) {
                                        selectedItems.put(i, true);
                                    }
                                }
                                notifyDataSetChanged();
                                mode.setTitle(selectList.size() + " Selected");
                            }
                            return false;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            isEnabled = false;
                            isSelectAll = false;
                            selectList.clear();
                            selectedItems.clear();
                            notifyDataSetChanged();
                            GridViewAdapter.this.mode = null;
                        }
                    };
                    ((AppCompatActivity) v.getContext()).startActionMode(callback);
                } else {
                    ClickItem(position);
                }
                return true;
            }
        });
        return convertView;
    }

    private void ClickItem(int position){
        DownloadModel downloadModel = arrayList.get(position);
        if (selectedItems.get(position)) {
            selectList.add(downloadModel);
        } else {
            selectList.remove(downloadModel);
        }

        if (mode != null) {
            if (selectList.size() == 0){
                mode.finish();
            }
            else{
                mode.setTitle(selectList.size() + " Selected");
            }
        }
    }

    public static class ViewHolder {
        ImageView imageView;
        TextView title;

        public ViewHolder(@NonNull View itemView) {
            title = itemView.findViewById(R.id.gridCaption);
            imageView = itemView.findViewById(R.id.gridImage);
        }
    }

    private void deleteSelectedFiles() {
        for (DownloadModel download : selectList) {
            FirebaseStorage.getInstance().getReferenceFromUrl(download.getFile_path()).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    arrayList.remove(download);
                    notifyDataSetChanged();
                    setLayoutVisibility(arrayList.size());
                    clearActionMode();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("GridViewAdapter", "Error deleting file: " + download.getFile_path(), e);
                }
            });
        }
    }

    // Set layout
    public void setLayoutVisibility(int quantityOfData){
        if(quantityOfData == 0){
            linearLayout2.setVisibility(View.GONE);
            linearLayout1.setVisibility(View.VISIBLE);
        } else {
            linearLayout1.setVisibility(View.GONE);
            linearLayout2.setVisibility(View.VISIBLE);
        }
    }

    // Clear ActionMode menu
    public void clearActionMode() {
        selectList.clear();
        selectedItems.clear();
        if (mode != null) {
            mode.finish();
        }
        notifyDataSetChanged();
    }
}

