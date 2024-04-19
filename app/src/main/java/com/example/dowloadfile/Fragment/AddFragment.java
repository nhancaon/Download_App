package com.example.dowloadfile.Fragment;

import android.Manifest;
import android.app.DownloadManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dowloadfile.Adapter.DownloadAdapter;
import com.example.dowloadfile.Model.DownloadModel;
import com.example.dowloadfile.R;
import com.example.dowloadfile.Utils.DownloadDBHelper;
import com.example.dowloadfile.Utils.ItemClickListener;
import com.example.dowloadfile.Utils.PathUtil;
import com.example.dowloadfile.Utils.RecyclerViewTouchHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddFragment extends Fragment implements AdapterView.OnItemClickListener, ItemClickListener {
    private static final int PERMISSION_REQUEST_CODE = 101;
    DownloadDBHelper dbHelper;
    String[] tabTitles;
    String file_name, status, progress, file_size, file_path;
    long downloadId;
    EditText fileName;
    EditText edtLink;
    List<DownloadModel> downloadModels = new ArrayList<>();
    Button add_download_list;
    RecyclerView data_list;
    DownloadAdapter downloadAdapter;
    private SearchView searchView;
    final private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Upload Firebase downloadedFile");
    final private StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    public AddFragment() {
        // Default constructor
    }

    public AddFragment(String[] tabTitles) {
        this.tabTitles = tabTitles;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);
        dbHelper = new DownloadDBHelper(requireActivity().getApplicationContext());
        restoreDownloadData();

        add_download_list = view.findViewById(R.id.add_download_list);
        data_list = view.findViewById(R.id.data_list);
        data_list.setLayoutManager(new LinearLayoutManager(requireContext()));

        add_download_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

        downloadAdapter = new DownloadAdapter(requireContext(),downloadModels, this, dbHelper, requireActivity().getSupportFragmentManager());
        data_list.setAdapter(downloadAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new RecyclerViewTouchHelper(downloadAdapter));
        itemTouchHelper.attachToRecyclerView(data_list);

        Intent intent = requireActivity().getIntent();
        if(intent != null){
            String action = intent.getAction();
            String type = intent.getType();
            if(Intent.ACTION_SEND.equals(action) && type != null){
                if(type.equalsIgnoreCase("text/plain")){
                    handleTextData(intent);
                }
                else if(type.startsWith("image/")){
                    handleImage(intent);
                }
                else if(type.equalsIgnoreCase("application/pdf")){
                    handlePdfFile(intent);
                }
            }
            else if(Intent.ACTION_SEND_MULTIPLE.equals(action) && type!=null){
                if(type.startsWith("image/")){
                    handleMultipleImage(intent);
                }
            }
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        requireContext().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Icons and text for each tab
        int[] tabIcons = {R.drawable.ic_close, R.drawable.ic_add, R.drawable.ic_complete};
        TabLayout tabLayout = requireActivity().findViewById(R.id.tab_layout);

        // Iterate through tabs and set custom view
        for (int i = 0; i < tabTitles.length; i++) {
            TabLayout.Tab tab = tabLayout.getTabAt(i);
            if (tab != null) {
                View customTabView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab_view, null);
                TextView tabText = customTabView.findViewById(R.id.tab_text);
                ImageView tabIcon = customTabView.findViewById(R.id.tab_icon);

                tabText.setText(tabTitles[i]);
                tabIcon.setImageResource(tabIcons[i]);
                tab.setCustomView(customTabView);
            }
        }
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if(tab.getPosition() == 0){
                    requireActivity().finish();
                    System.exit(0);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);

        SearchManager searchManager = (SearchManager) requireActivity().getSystemService (Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                downloadAdapter.getFilter().filter(query);
                return false;
            }
            @Override
            public boolean onQueryTextChange (String newText) {
                downloadAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    /// Outside onViewCreated
    private void uploadToFirebase(Uri uri){
        final StorageReference imageReference = storageReference.child(System.currentTimeMillis() + "." + getFileExtension(Uri.parse(file_path)));

        imageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                imageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        DownloadModel downloadModel = new DownloadModel();
                        downloadModel.setId(downloadModel.getId());
                        downloadModel.setStatus(status);
                        downloadModel.setTitle(extractFileName(file_name));
                        downloadModel.setFile_size(file_size);
                        downloadModel.setProgress(progress);
                        downloadModel.setIs_paused(true);
                        downloadModel.setDownloadId(downloadId);
                        downloadModel.setFile_path(file_path);

                        String key = databaseReference.push().getKey();
                        databaseReference.child(key).setValue(downloadModel);
                        Toast.makeText(requireActivity(), "Firebase uploaded", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Extract file type
    public static String extractFileName(String title) {
        // Split the title by dot (.)
        String[] parts = title.split("\\.");

        // If there's only one part or the last part is empty, return the original title
        if (parts.length <= 1 || parts[parts.length - 1].isEmpty()) {
            return title;
        }

        // Check if the last part has more dots
        String lastPart = parts[parts.length - 1];
        if (lastPart.indexOf('.') != lastPart.lastIndexOf('.')) {
            return title;
        }
        return parts[0];
    }

    private String getFileExtension(Uri fileUri) {
        String path = fileUri.toString();

        // Extract the file extension based on the type of URI
        if (path.startsWith("file://")) {
            // Local file path
            File file = new File(path);
            String fileName = file.getName();
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
                return fileName.substring(lastDotIndex + 1);
            }
        } else {
            // Web URL
            int lastDotIndex = path.lastIndexOf('.');
            if (lastDotIndex != -1 && lastDotIndex < path.length() - 1) {
                String extension = path.substring(lastDotIndex + 1);
                // Ensure that the extension does not contain any query parameters or slashes
                int queryParamIndex = extension.indexOf('?');
                if (queryParamIndex != -1) {
                    extension = extension.substring(0, queryParamIndex);
                }
                int slashIndex = extension.indexOf('/');
                if (slashIndex != -1) {
                    extension = extension.substring(0, slashIndex);
                }
                return extension;
            }
        }
        return null;
    }

    private void handlePdfFile(Intent intent) {
        Uri pdffile = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (pdffile != null) {
            Log.d("PDF File Path : ", "" + pdffile.getPath());
        }
    }

    private void handleImage(Intent intent) {
        Uri image = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (image != null) {
            Log.d("Image File Path : ", "" + image.getPath());
        }
    }

    private void handleTextData(Intent intent) {
        String textdata = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (textdata != null) {
            Log.d("Text Data : ", "" + textdata);
            downloadFile(textdata);
        }
    }

    private void handleMultipleImage(Intent intent) {
        ArrayList<Uri> imageList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageList != null) {
            for (Uri uri : imageList) {
                Log.d("Path ", "" + uri.getPath());
            }
        }
    }

    private void showInputDialog() {
        AlertDialog.Builder al = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.activity_input_dialog, null);
        al.setView(view);

        edtLink = view.findViewById(R.id.edtLink);
        fileName = view.findViewById(R.id.edtFileName);
        Button paste = view.findViewById(R.id.paste);

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                try {
                    CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                    edtLink.setText(charSequence);
                    file_path = edtLink.getText().toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        al.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadFile(edtLink.getText().toString());
            }
        });

        al.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        al.show();
    }

    // CAO LÀM SO SÁNH THỜI GIAN (Log ra consolde)
    private void downloadFile(String url) {
        String nameFromURL = URLUtil.guessFileName(url, null, null);
        String extension = FilenameUtils.getExtension(nameFromURL);

        if (!isValidFileType(extension)) {
            Toast.makeText(requireContext(), "Unsupported file type", Toast.LENGTH_SHORT).show();
            return;
        }

        file_name = fileName.getText().toString().trim();
        if (file_name.isEmpty()) {
            Toast.makeText(requireContext(), "File name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        file_name += "." + extension;
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        File file = new File(downloadPath,file_name);

        DownloadManager.Request request = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(file_name)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }
        else{
            request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(file_name)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }

        DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = downloadManager.enqueue(request);

        final DownloadModel downloadModel = new DownloadModel();
        downloadModel.setStatus("Downloading");
        downloadModel.setTitle(file_name);
        downloadModel.setFile_size("0");
        downloadModel.setProgress("0");
        downloadModel.setIs_paused(false);
        downloadModel.setDownloadId(downloadId);
        downloadModel.setFile_path("");

        downloadModels.add(downloadModel);
        downloadAdapter.notifyItemInserted(downloadModels.size()-1);

        DownloadStatusTask downloadStatusTask = new DownloadStatusTask(downloadModel);
        runTask(downloadStatusTask, "" + downloadId);
    }

    public class DownloadStatusTask extends AsyncTask<String, String, String> {
        DownloadModel downloadModel;
        public DownloadStatusTask(DownloadModel downloadModel){
            this.downloadModel = downloadModel;
        }

        @Override
        protected String doInBackground(String... strings) {
            downloadFileProcess(strings[0]);
            return null;
        }

        private void downloadFileProcess(String downloadId) {
            DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
            boolean downloading = true;

            while (downloading) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(Long.parseLong(downloadId));
                Cursor cursor = downloadManager.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    int bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);

                    if (bytesDownloadedIndex >= 0 && totalSizeIndex >= 0 && statusIndex >= 0) {
                        int bytes_downloaded = cursor.getInt(bytesDownloadedIndex);
                        int total_size = cursor.getInt(totalSizeIndex);
                        int status = cursor.getInt(statusIndex);

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false;
                        }

                        int progress;
                        if (total_size != 0) {
                            progress = (int) ((bytes_downloaded * 100L) / total_size);
                        } else {
                            progress = 0; // or handle it according to your logic
                        }
                        String statusMessage = getStatusMessage(status);
                        publishProgress(new String[]{String.valueOf(progress), String.valueOf(bytes_downloaded), statusMessage});
                    }
                    cursor.close();
                } else {
                    // Handle the case where the cursor is null or empty
                    // You might want to add logging or error handling here
                }
            }
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            super.onProgressUpdate(values);
            this.downloadModel.setFile_size(bytesIntoHumanReadable(Long.parseLong(values[1])));
            file_size = bytesIntoHumanReadable(Long.parseLong(values[1]));

            this.downloadModel.setProgress(values[0]);
            progress = values[0];
            if (!downloadModel.getStatus().equalsIgnoreCase("PAUSE") && !downloadModel.getStatus().equalsIgnoreCase("RESUME")) {
                downloadModel.setStatus(values[2]);
                status = values[2];
            }
            downloadAdapter.changeItem(downloadModel.getDownloadId());
        }
    }

    private String getStatusMessage(int status) {
        String msg = "-";
        switch (status) {
            case DownloadManager.STATUS_FAILED:
                msg = "Failed";
                break;
            case DownloadManager.STATUS_PAUSED:
                msg = "Paused";
                break;
            case DownloadManager.STATUS_RUNNING:
                msg = "Running";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Completed";
                break;
            case DownloadManager.STATUS_PENDING:
                msg = "Pending";
                break;
            default:
                msg = "Unknown";
                break;
        }
        return msg;
    }

    BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            boolean comp = downloadAdapter.ChangeItemWithStatus("Completed", id);

            if (comp) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
                Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));

                if (cursor != null && cursor.moveToFirst()) {
                    int localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                    if (localUriIndex >= 0) {
                        String downloaded_path = cursor.getString(localUriIndex);
                        downloadAdapter.setChangeItemFilePath(downloaded_path, id);
                        // Upload the downloaded file to Firebase
                        uploadToFirebase(Uri.parse(downloaded_path));
                    } else {
                        // Handle the case where the COLUMN_LOCAL_URI column doesn't exist
                    }
                    cursor.close();
                } else {
                    // Handle the case where the cursor is null or empty
                }
                for (DownloadModel downloadModel : downloadModels) {
                    if (downloadModel.getDownloadId() == id) {
                        // Save the download to the database
                        dbHelper.insertDownload(downloadModel);
                        break;
                    }
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null) {
            getActivity().unregisterReceiver(onComplete);
        }
    }

    public void runTask(DownloadStatusTask downloadStatusTask, String id) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                downloadStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, id);
            } else {
                downloadStatusTask.execute(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String bytesIntoHumanReadable(long bytes) {
        long kilobyte = 1024;
        long megabyte = kilobyte * 1024;
        long gigabyte = megabyte * 1024;
        long terabyte = gigabyte * 1024;

        if ((bytes >= 0) && (bytes < kilobyte)) {
            return bytes + " B";

        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            return (bytes / kilobyte) + " KB";

        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            return (bytes / megabyte) + " MB";

        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            return (bytes / gigabyte) + " GB";

        } else if (bytes >= terabyte) {
            return (bytes / terabyte) + " TB";

        } else {
            return bytes + " Bytes";
        }
    }

    private void restoreDownloadData() {
        downloadModels.clear();
        downloadModels.addAll(dbHelper.getAllDownload());
    }

    @Override
    public void onCLickItem(String file_path) {
        Log.d("File Path : ", "" + file_path);
        openFile(file_path);
    }

    @Override
    public void onShareClick(DownloadModel downloadModel) {
        File file = new File(downloadModel.getFile_path().replaceAll("file:///", ""));
        Log.d("File Path", "" + file.getAbsolutePath());
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String type = mimeTypeMap.getExtensionFromMimeType(ext);

        if (type == null) {
            type = "*/*";
        }

        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, "Sharing File from File Downloader");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri path = FileProvider.getUriForFile(requireActivity().getApplicationContext(),
                        "com.example.dowloadfile", file);
                intent.putExtra(Intent.EXTRA_STREAM, path);
            } else {
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            }
            intent.setType("*/*");
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireActivity().getApplicationContext(), "No Activity Availabe to Handle File",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void openFile(String fileurl) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!checkPermission()) {
                requestPermission();
                Toast.makeText(requireActivity().getApplicationContext(), "Please Allow Permission to Open File",
                        Toast.LENGTH_SHORT).show();
                return;
            }

        }

        try {
            fileurl = PathUtil.getPath(requireActivity().getApplicationContext(), Uri.parse(fileurl));

            File file = new File(fileurl);
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
            String type = mimeTypeMap.getMimeTypeFromExtension(ext);

            if (type == null) {
                type = "*/*";
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri contne = FileProvider.getUriForFile(requireActivity().getApplicationContext(),
                        "com.example.dowloadfile", file);
                intent.setDataAndType(contne, type);
            } else {
                intent.setDataAndType(Uri.fromFile(file), type);
            }
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireActivity().getApplicationContext(), "Unable to Open File", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(requireActivity().getApplicationContext(), "Please Give Permission to Upload File",
                    Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireActivity().getApplicationContext(), "Permission Successfull", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireActivity().getApplicationContext(), "Permission Failed", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(requireActivity().getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isValidFileType(String extension) {
        return extension.equalsIgnoreCase("docx") ||
                extension.equalsIgnoreCase("pdf") ||
                extension.equalsIgnoreCase("jpg") ||
                extension.equalsIgnoreCase("jpeg") ||
                extension.equalsIgnoreCase("png") ||
                extension.equalsIgnoreCase("mp4") ||
                extension.equalsIgnoreCase("avi") ||
                extension.equalsIgnoreCase("html");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check if there are any downloads in progress and update their status
        for (DownloadModel downloadModel : downloadModels) {
            if (downloadModel.getStatus().equals("Running")) {
                // Resume the AsyncTask to monitor download progress
                DownloadStatusTask downloadStatusTask = new DownloadStatusTask(downloadModel);
                runTask(downloadStatusTask, "" + downloadModel.getDownloadId());
            }
        }
    }
}
