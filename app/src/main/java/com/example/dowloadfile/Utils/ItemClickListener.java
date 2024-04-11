package com.example.dowloadfile.Utils;

import com.example.dowloadfile.Model.DownloadModel;

public interface ItemClickListener {
    void onCLickItem(String file_path);
    void onShareClick(DownloadModel downloadModel);
}
