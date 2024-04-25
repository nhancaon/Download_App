package com.example.dowloadfile.Fragment;

import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.example.dowloadfile.Adapter.GridViewAdapter;
import com.example.dowloadfile.Model.DownloadModel;
import com.example.dowloadfile.R;
import com.example.dowloadfile.Utils.GridItemClickListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadFragment extends Fragment implements GridItemClickListener {
    private GridView gridView;
    private ArrayList<DownloadModel> dataList;
    private GridViewAdapter adapter;
    private Button btnDownloadAndAddImages;
    private Spinner spinnerFolder;
    ArrayList<String> folderNames = new ArrayList<>();
    private  ArrayAdapter<String> spinnerAdapter;
    private TextView txtDownloadTime;
    private LinearLayout linearLayout1, linearLayout2;
    private SearchView searchView;
    public DownloadFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        dataList = new ArrayList<>();
        gridView = view.findViewById(R.id.gridView);
        linearLayout1 = view.findViewById(R.id.linearLayout1);
        linearLayout2 = view.findViewById(R.id.linearLayout2);

        adapter = new GridViewAdapter(requireContext(), dataList, this, linearLayout1, linearLayout2);
        gridView.setAdapter(adapter);

        spinnerFolder = view.findViewById(R.id.spinnerFolder);
        txtDownloadTime = view.findViewById(R.id.txtDownloadTime);
        btnDownloadAndAddImages = view.findViewById(R.id.btnDownloadAndAddImages);

        FirebaseStorage.getInstance().getReference().listAll().addOnSuccessListener(listResult -> {
            for (StorageReference prefix : listResult.getPrefixes()) {
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
        setHasOptionsMenu(true);

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
                downloadAllImagesInFolder();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

        SearchManager searchManager = (SearchManager) requireActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adapter != null) {
            adapter.clearActionMode();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.clearActionMode();
        }
    }

    private void downloadAllImagesInFolder() {
        long startTime = System.currentTimeMillis();

        // Check if spinnerFolder is not null and contains elements
        if (spinnerFolder != null && spinnerFolder.getAdapter() != null && spinnerFolder.getAdapter().getCount() > 0) {
            FirebaseStorage.getInstance().getReference().child(spinnerFolder.getSelectedItem().toString()).listAll().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<StorageReference> items = task.getResult().getItems();
                    Toast.makeText(requireContext(), "All files selected in folder was queue", Toast.LENGTH_SHORT).show();
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
        Handler handler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            boolean downloading = true;
            while (downloading) {
                // using ID to check download file status because many file download so shoulde be using thread to check status each file
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

                            // Convert the time to a suitable format (e.g., seconds, minutes)
                            String downloadTime = formatDownloadTime(elapsedTime);

                            // Display the total download time on TextView
                            handler.post(() -> txtDownloadTime.setText(getString(R.string.complete_download) + " " + downloadTime));
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

    private void loadImagesFromFolder(String folderName) {
        dataList.clear();

        FirebaseStorage.getInstance().getReference().child(folderName).listAll().addOnCompleteListener(new OnCompleteListener<ListResult>() {
            @Override
            public void onComplete(@NonNull Task<ListResult> task) {
                if (task.isSuccessful()) {
                    ListResult listResult = task.getResult();
                    adapter.setLayoutVisibility(listResult.getItems().size());
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
                                    // Sort the dataList based on title
                                    Collections.sort(dataList, new Comparator<DownloadModel>() {
                                        @Override
                                        public int compare(DownloadModel o1, DownloadModel o2) {
                                            String title1 = o1.getTitle();
                                            String title2 = o2.getTitle();

                                            // Split titles into parts containing numbers and non-numbers
                                            String[] parts1 = title1.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
                                            String[] parts2 = title2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

                                            // Compare each part sequentially
                                            int i = 0;
                                            while (i < parts1.length && i < parts2.length) {
                                                // If both parts are numeric, compare them as integers
                                                if (parts1[i].matches("\\d+") && parts2[i].matches("\\d+")) {
                                                    int num1 = Integer.parseInt(parts1[i]);
                                                    int num2 = Integer.parseInt(parts2[i]);
                                                    int result = Integer.compare(num1, num2);
                                                    if (result != 0) {
                                                        return result;
                                                    }
                                                } else {
                                                    // Compare parts as strings
                                                    int result = parts1[i].compareToIgnoreCase(parts2[i]);
                                                    if (result != 0) {
                                                        return result;
                                                    }
                                                }
                                                i++;
                                            }

                                            // If all parts are the same up to this point, compare lengths
                                            return parts1.length - parts2.length;
                                        }
                                    });
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

    @Override
    public void onItemLongClick(int position) {

    }
}