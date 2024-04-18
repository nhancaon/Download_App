package com.example.dowloadfile.Fragment;
import static com.example.dowloadfile.Fragment.AddFragment.extractFileName;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadFragment extends Fragment {

    private GridView gridView;
    private ArrayList<DownloadModel> dataList;
    private GridViewAdapter adapter;
    private  Button btnUploadToFirebase;
    private Button btnDownloadAndAddImages;
    private Spinner spinnerFolder;
    ArrayList<String> folderNames = new ArrayList<>();
    private  ArrayAdapter<String> spinnerAdapter;
    private TextView tvDownloadTime;
    private TextView tvUploadTime;
    private long totalDownloadTime = 0;
    private long totalUploadTime = 0;
    public DownloadFragment() {
        // Required empty public constructor
    }
    private static final int REQUEST_CODE_PICK_FOLDER = 101;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_download, container, false);
        gridView = view.findViewById(R.id.gridView);
        dataList = new ArrayList<>();
        adapter = new GridViewAdapter(requireContext(), dataList);
        gridView.setAdapter(adapter);
        spinnerFolder = view.findViewById(R.id.spinnerFolder);

        // Tìm TextView bằng ID
        tvDownloadTime = view.findViewById(R.id.tvDownloadTime);
        tvUploadTime = view.findViewById(R.id.tvUploadTime);
        // Lấy reference đến thư mục gốc trên Firebase Storage
        StorageReference rootRef = FirebaseStorage.getInstance().getReference();

        // Lấy danh sách tất cả các item trực tiếp trong thư mục gốc (bao gồm cả folder và file)
        rootRef.listAll().addOnSuccessListener(listResult -> {
            for (StorageReference prefix : listResult.getPrefixes()) {
                // Lấy tên của folder và thêm vào danh sách
                String folderName = prefix.getName();
                folderNames.add(folderName);
                // Cập nhật adapter của Spinner sau khi đã thêm folder vào danh sách
                spinnerAdapter.notifyDataSetChanged();
            }
        }).addOnFailureListener(e -> {
            // Xử lý khi không thể lấy danh sách item trực tiếp trong thư mục gốc
            Toast.makeText(requireContext(), "Failed to get folder", Toast.LENGTH_SHORT).show();
        });

        // Khởi tạo adapter cho Spinner
        spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, folderNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFolder.setAdapter(spinnerAdapter);

        spinnerFolder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Lấy tên của thư mục được chọn
                String selectedFolderName = spinnerFolder.getSelectedItem().toString();
                // Tiến hành tải danh sách hình ảnh từ thư mục đã chọn
                loadImagesFromFolder(selectedFolderName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Không cần thực hiện gì khi không có mục nào được chọn
            }
        });

        btnUploadToFirebase = view.findViewById(R.id.btnUploadDataToFirebase);
        btnUploadToFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalUploadTime = 0;
                // Gọi phương thức uploadToFirebase khi click vào nút
                pickFolderAndUploadToFirebase();
            }
        });

        // Khởi tạo và lắng nghe sự kiện click cho nút
        btnDownloadAndAddImages = view.findViewById(R.id.btnDownloadAndAddImages);
        btnDownloadAndAddImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                totalDownloadTime = 0;
                // Gọi phương thức để tải xuống và thêm hình ảnh
                downloadAndAddImages();
            }
        });
        return view;
    }

    private void pickFolderAndUploadToFirebase() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK && data != null) {
            Uri folderUri = data.getData();
            if (folderUri != null) {
                uploadFilesFromFolder(folderUri);
            }
        }
    }

    private void uploadFilesFromFolder(Uri folderUri) {
        Cursor cursor = requireActivity().getContentResolver().query(
                folderUri,
                null,
                null,
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Lấy URI của file từ Cursor
                String fileUriString = cursor.getString(cursor.getColumnIndex(MediaStore.Downloads.DATA));
                Uri fileUri = Uri.parse(fileUriString);

                // Upload file lên Firebase Storage
                uploadToFirebase(fileUri);
            } while (cursor.moveToNext());
            cursor.close(); // Đóng Cursor sau khi sử dụng
        }
    }

    private void uploadToFirebase(Uri fileUri) {
        // Tạo một storage reference từ FirebaseStorage
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        // Tạo một reference đến vị trí bạn muốn lưu trữ tệp trên Firebase Storage
        StorageReference fileRef = storageRef.child("uploads/" + fileUri.getLastPathSegment());

        // Tải lên tệp lên Firebase Storage
        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Tải lên thành công
                    Toast.makeText(requireContext(), "Upload success", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(exception -> {
                    // Xảy ra lỗi khi tải lên
                    Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                });
    }


    private void downloadAndAddImages() {
        String selectedFolderName = "images"; // Default folder name
        long startTime = System.currentTimeMillis();

        // Check if spinnerFolder is not null
        if (spinnerFolder != null) {
            // Get the selected folder name from the spinner
            selectedFolderName = spinnerFolder.getSelectedItem().toString();
        }
        // Lấy reference đến thư mục chứa tất cả các hình ảnh trên Firebase Storage
        StorageReference imagesRef = FirebaseStorage.getInstance().getReference().child(selectedFolderName);

        // Thực hiện lấy danh sách tất cả các file trong thư mục
        imagesRef.listAll().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<StorageReference> items = task.getResult().getItems();
                for (StorageReference item : items) {
                    // Lấy tên của từng file
                    String fileName = item.getName();
                    // Lấy URL của file
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Tạo một request để tải xuống file
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
                        // Xử lý lỗi nếu không thể lấy URL của file
                        Toast.makeText(requireContext(), "Failed to get download URL", Toast.LENGTH_SHORT).show();
                    });
                }
                Toast.makeText(requireContext(), "All images have been queued for download", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to list files", Toast.LENGTH_SHORT).show();
            }
        });
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

                            // Hiển thị thời gian đếm hoàn thành trên TextView
                            tvDownloadTime.post(() -> tvDownloadTime.setText("Download completed in: " + downloadTime));

                            downloading = false;
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            // Xử lý trường hợp tải xuống thất bại
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
        // Tính toán thời gian trong giây hoặc phút, giây, tùy thuộc vào yêu cầu của bạn
        // Ví dụ:
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return minutes + " minutes " + seconds + " seconds";
        } else {
            return seconds + " seconds";
        }
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
            // Continue with your logic using selectedFolderName
            FirebaseStorage.getInstance().getReference().child("images").listAll().addOnCompleteListener(new OnCompleteListener<ListResult>() {
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
                                        // Add the downloadModel to your dataList
                                        dataList.add(downloadModel);
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

    private void loadImagesFromFolder(String folderName) {
        // Xóa danh sách hình ảnh cũ
        dataList.clear();

        // Lấy reference đến thư mục chứa tất cả các hình ảnh trên Firebase Storage
        StorageReference imagesRef = FirebaseStorage.getInstance().getReference().child(folderName);

        // Thực hiện lấy danh sách tất cả các file trong thư mục
        imagesRef.listAll().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<StorageReference> items = task.getResult().getItems();
                for (StorageReference item : items) {
                    // Lấy tên của từng file
                    String fileName = item.getName();
                    // Lấy URL của file
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Tạo một đối tượng DownloadModel để lưu trữ thông tin về hình ảnh
                        DownloadModel downloadModel = new DownloadModel();
                        downloadModel.setFile_path(uri.toString());
//                        downloadModel.setFile_name(fileName);
                        // Add đối tượng DownloadModel vào danh sách
                        dataList.add(downloadModel);
                        // Thông báo cho adapter rằng dữ liệu đã thay đổi
                        adapter.notifyDataSetChanged();
                    }).addOnFailureListener(e -> {
                        // Xử lý lỗi nếu không thể lấy URL của file
                        Toast.makeText(requireContext(), "Failed to get download URL", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                Toast.makeText(requireContext(), "Failed to list files", Toast.LENGTH_SHORT).show();
            }
        });

    }
}