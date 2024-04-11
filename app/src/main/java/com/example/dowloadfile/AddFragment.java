package com.example.dowloadfile;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class AddFragment extends Fragment implements AdapterView.OnItemClickListener {
    DownloadAdapter downloadAdapter;
    Button add_download_list;
    RecyclerView data_list;
    List<DownloadModel> downloadModels = new ArrayList<>();
    String[] tabTitles;

    public AddFragment(String[] tabTitles) {
        this.tabTitles = tabTitles;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);
        add_download_list = view.findViewById(R.id.add_download_list);
        data_list = view.findViewById(R.id.data_list);

        data_list.setLayoutManager(new LinearLayoutManager(requireContext()));

        add_download_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });
        downloadAdapter = new DownloadAdapter(requireContext(), downloadModels);
        data_list.setAdapter(downloadAdapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireContext().registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        // Assuming you have icons and text for each tab
        int[] tabIcons = {R.drawable.ic_close, R.drawable.ic_settings, R.drawable.ic_url, R.drawable.ic_add, R.drawable.ic_download_queue, R.drawable.ic_complete, R.drawable.ic_menu};
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
                if(tab.getPosition()==0){
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

//        newsArrayList = new ArrayList<>();
//        // Change the initial progress value from "progress" to "0"
//        newsArrayList.add(new DownloadModel(1, 1, "society", "///", "0", "status", "dsdsd", false));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    /////////////////////////
    private void handlePDFFile(Intent intent) {
        Uri pdf_file = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(pdf_file != null) {
            Log.d("Pdf File Path : ", "" + pdf_file.getPath());
        }
    }

    private void handleImage(Intent intent) {
        Uri image = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(image != null) {
            Log.d("Image File Path : ", "" + image.getPath());
        }
    }

    private void handleTextData(Intent intent) {
        String textdata = intent.getStringExtra(Intent.EXTRA_TEXT);
        if(textdata != null) {
            Log.d("Text Data : ", "" + textdata);
            downloadFile(textdata);
        }
    }

    private void handleMultipleImage(Intent intent) {
        ArrayList<Uri> imageList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if(imageList!=null) {
            for (Uri uri : imageList) {
                Log.d("Path ","" + uri.getPath());
            }
        }
    }

    private void showInputDialog() {
        AlertDialog.Builder al = new AlertDialog.Builder(requireContext());
        View view = getLayoutInflater().inflate(R.layout.input_dialog, null);
        al.setView(view);

        final EditText editText = view.findViewById(R.id.input);
        Button paste = view.findViewById(R.id.paste);

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                try {
                    CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                    editText.setText(charSequence);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        al.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadFile(editText.getText().toString());
                dialog.dismiss();
            }
        });

        al.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        al.show();
    }

    private void downloadFile(String url) {
        String filename = URLUtil.guessFileName(url,null,null);
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        File file = new File(downloadPath,filename);

        DownloadManager.Request request = null;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }
        else{
            request = new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }

        DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = downloadManager.enqueue(request);

        final DownloadModel downloadModel = new DownloadModel(1, 1, "society", "///", "progress", "status", "dsdsd", false);
        downloadModel.setId(11);
        downloadModel.setStatus("Downloading");
        downloadModel.setTitle(filename);
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
            this.downloadModel=downloadModel;
        }

        @Override
        protected String doInBackground(String... strings) {
            downloadFileProcess(strings[0]);
            return null;
        }

        private void downloadFileProcess(String downloadId) {
            DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
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
            this.downloadModel.setProgress(values[0]);
            if (!downloadModel.getStatus().equalsIgnoreCase("PAUSE") && !downloadModel.getStatus().equalsIgnoreCase("RESUME")) {
                downloadModel.setStatus(values[2]);
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
                DownloadManager downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
                Cursor cursor = downloadManager.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    int localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);

                    if (localUriIndex >= 0) {
                        String downloaded_path = cursor.getString(localUriIndex);
                        downloadAdapter.setChangeItemFilePath(downloaded_path, id);
                    } else {
                        // Handle the case where the COLUMN_LOCAL_URI column doesn't exist
                    }

                    cursor.close();
                } else {
                    // Handle the case where the cursor is null or empty
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
}
