package com.example.dowloadfile.Fragment;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.dowloadfile.Adapter.GridViewAdapter;
import com.example.dowloadfile.Model.DownloadModel;
import com.example.dowloadfile.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DownloadFragment extends Fragment {
    private GridView gridView;
    private ArrayList<DownloadModel> dataList;
    private GridViewAdapter adapter;
    private Button btnDownloadAndAddImages;
    private Spinner spinnerFolder;
    ArrayList<String> folderNames = new ArrayList<>();
    private  ArrayAdapter<String> spinnerAdapter;
    private TextView txtDownloadTime;
    private long totalDownloadTime = 0;
    public DownloadFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        gridView = view.findViewById(R.id.gridView);
        dataList = new ArrayList<>();
        adapter = new GridViewAdapter(requireContext(), dataList);
        gridView.setAdapter(adapter);

        spinnerFolder = view.findViewById(R.id.spinnerFolder);
        txtDownloadTime = view.findViewById(R.id.txtDownloadTime);
        btnDownloadAndAddImages = view.findViewById(R.id.btnDownloadAndAddImages);

        // Lấy danh sách tất cả các item trực tiếp trong thư mục gốc (bao gồm cả folder và file)
        FirebaseStorage.getInstance().getReference().listAll().addOnSuccessListener(listResult -> {
            for (StorageReference prefix : listResult.getPrefixes()) {
                // Lấy tên của folder và thêm vào danh sách
                folderNames.add(prefix.getName());
                spinnerAdapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(requireContext(), "Failed to get folder", Toast.LENGTH_SHORT).show();
        });

        spinnerAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_item, folderNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFolder.setAdapter(spinnerAdapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerFolder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                loadImagesFromFolder(spinnerFolder.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }
        });

        btnDownloadAndAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalDownloadTime = 0;
                downloadAllImagesInFolder();
            }
        });
    }

    private void downloadAllImagesInFolder() {
        long startTime = System.currentTimeMillis();

        // Check if spinnerFolder is not null and contains elements
        if (spinnerFolder != null && spinnerFolder.getAdapter() != null && spinnerFolder.getAdapter().getCount() > 0) {
            FirebaseStorage.getInstance().getReference().child(spinnerFolder.getSelectedItem().toString()).listAll().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<StorageReference> items = task.getResult().getItems();
                    for (StorageReference item : items) {
                        String fileName = item.getName();

                        // Lấy URL của file
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            DownloadManager.Request request = new DownloadManager.Request(uri)
                                    .setTitle(fileName)
                                    .setDescription("Downloading")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                            // Thêm request vào DownloadManager để bắt đầu quá trình tải xuống
                            DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                            long downloadId = downloadManager.enqueue(request);

                            // Kiểm tra trạng thái của tải xuống
                            checkDownloadStatus(downloadManager, downloadId, startTime);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed to get download URL", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to list files", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(requireContext(), "No folders available to download", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkDownloadStatus(DownloadManager downloadManager, long downloadId, long startTime) {
        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = downloadManager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    if (statusIndex != -1) {
                        int status = cursor.getInt(statusIndex);
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            long endTime = System.currentTimeMillis();
                            long elapsedTime = endTime - startTime;

                            // Thêm thời gian download của mỗi file vào totalDownloadTime
                            totalDownloadTime += elapsedTime;

                            // Chuyển đổi thời gian thành định dạng phù hợp (ví dụ: giây, phút)
                            String downloadTime = formatDownloadTime(totalDownloadTime);
                            txtDownloadTime.post(() -> txtDownloadTime.setText("Download completed in: " + downloadTime));

                            downloading = false;
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloading = false;
                        }
                    }
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
        }).start();
    }

    private String formatDownloadTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return minutes + " minutes " + seconds + " seconds";
        } else {
            return seconds + " seconds";
        }
    }

    // Ngon
    private void loadImagesFromFolder(String folderName) {
        dataList.clear();

        FirebaseStorage.getInstance().getReference().child(folderName).listAll().addOnCompleteListener(new OnCompleteListener<ListResult>() {
            @Override
            public void onComplete(@NonNull Task<ListResult> task) {
                if (task.isSuccessful()) {
                    ListResult listResult = task.getResult();
                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String url = task.getResult().toString();

                                    DownloadModel downloadModel = new DownloadModel();
                                    downloadModel.setFile_path(url);
                                    downloadModel.setTitle(item.getName());

                                    // Add the downloadModel to your dataList
                                    dataList.add(downloadModel);
                                    // Sort the dataList based on title or any other criteria
                                    Collections.sort(dataList, new Comparator<DownloadModel>() {
                                        @Override
                                        public int compare(DownloadModel o1, DownloadModel o2) {
                                            // You can change the sorting criteria here
                                            return o1.getTitle().compareTo(o2.getTitle());
                                        }
                                    });
                                    Integer retrieveQuantity = dataList.size();
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(requireContext(), "Failed to retrieve image URL", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to list files", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}