package com.example.dowloadfile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.DownloadManager;
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
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity{

    DownloadAdapter downloadAdapter;
    List<DownloadModel> downloadModels=new ArrayList<>();
    //Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(onComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        Button add_download_list=findViewById(R.id.add_download_list);
        RecyclerView data_list=findViewById(R.id.data_list);

        add_download_list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

        downloadAdapter=new DownloadAdapter(MainActivity.this,downloadModels);
        data_list.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        data_list.setAdapter(downloadAdapter);
    }

    private void handlePdfFile(Intent intent) {
        Uri pdffile=intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(pdffile!=null) {
            Log.d("Pdf File Path : ", "" + pdffile.getPath());
        }
    }

    private void handleImage(Intent intent) {
        Uri image=intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if(image!=null) {
            Log.d("Image File Path : ", "" + image.getPath());
        }
    }

    private void handleTextData(Intent intent) {
        String  textdata=intent.getStringExtra(Intent.EXTRA_TEXT);
        if(textdata!=null) {
            Log.d("Text Data : ", "" + textdata);
            downloadFile(textdata);
        }
    }

    private void handleMultipleImage(Intent intent) {
        ArrayList<Uri> imageList=intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if(imageList!=null) {
            for (Uri uri : imageList) {
                Log.d("Path ",""+uri.getPath());
            }
        }
    }


    private void showInputDialog(){
        AlertDialog.Builder al=new AlertDialog.Builder(MainActivity.this);
        View view=getLayoutInflater().inflate(R.layout.input_dialog,null);
        al.setView(view);


        final EditText editText=view.findViewById(R.id.input);
        Button paste=view.findViewById(R.id.paste);

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager= (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                try{
                    CharSequence charSequence=clipboardManager.getPrimaryClip().getItemAt(0).getText();
                    editText.setText(charSequence);
                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        al.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadFile(editText.getText().toString());
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
        String filename= URLUtil.guessFileName(url,null,null);
        String downloadPath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        File file=new File(downloadPath,filename);

        DownloadManager.Request request=null;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            request=new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }
        else{
            request=new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }

        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long downloadId=downloadManager.enqueue(request);

        final DownloadModel downloadModel=new DownloadModel();
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


        DownloadStatusTask downloadStatusTask=new DownloadStatusTask(downloadModel);
        runTask(downloadStatusTask,""+downloadId);
    }

    public class DownloadStatusTask extends AsyncTask<String,String,String>{

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
            DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            boolean downloading=true;
            while (downloading){
                DownloadManager.Query query=new DownloadManager.Query();
                query.setFilterById(Long.parseLong(downloadId));
                Cursor cursor=downloadManager.query(query);
                cursor.moveToFirst();

                int bytes_downloaded=cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                int total_size=cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                if(cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))==DownloadManager.STATUS_SUCCESSFUL){
                    downloading=false;
                }

                int progress= (int) ((bytes_downloaded*100L)/total_size);
                String status=getStatusMessage(cursor);
                publishProgress(new String[]{String.valueOf(progress), String.valueOf(bytes_downloaded),status});
                cursor.close();
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

    private String getStatusMessage(Cursor cursor) {
        String msg="-";
        switch (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))){
            case DownloadManager.STATUS_FAILED:
                msg="Failed";
                break;
            case DownloadManager.STATUS_PAUSED:
                msg= "Paused";
                break;
            case DownloadManager.STATUS_RUNNING:
                msg= "Running";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                msg= "Completed";
                break;
            case DownloadManager.STATUS_PENDING:
                msg= "Pending";
                break;
            default:
                msg="Unknown";
                break;
        }
        return msg;
    }

    BroadcastReceiver onComplete=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
            boolean comp=downloadAdapter.ChangeItemWithStatus("Completed",id);

            if(comp){
                DownloadManager.Query query=new DownloadManager.Query();
                query.setFilterById(id);
                DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Cursor cursor=downloadManager.query(new DownloadManager.Query().setFilterById(id));
                cursor.moveToFirst();

                String downloaded_path=cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                downloadAdapter.setChangeItemFilePath(downloaded_path,id);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

    public void runTask(DownloadStatusTask downloadStatusTask,String id){
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
                downloadStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new String[]{id});
            }
            else{
                downloadStatusTask.execute(new String[]{id});
            }
        }
        catch (Exception e){
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
