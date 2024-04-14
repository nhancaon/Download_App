package com.example.dowloadfile.Utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.dowloadfile.Model.DownloadModel;

import java.util.ArrayList;
import java.util.List;

public class DownloadDBHelper extends SQLiteOpenHelper {
    private SQLiteDatabase db;
    private static final String DATABASE_NAME = "downloadDB.db";
    private static final int DATABASE_VERSION = 1;

    public DownloadDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS downloads (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "download_id INTEGER," +
                        "title TEXT," +
                        "file_path TEXT," +
                        "progress TEXT," +
                        "status TEXT," +
                        "file_size TEXT," +
                        "is_paused INTEGER)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS downloads");
        onCreate(db);
    }


    public void insertDownload(DownloadModel downloadModel) {
        try {
            db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("download_id", downloadModel.getDownloadId());
            values.put("title", downloadModel.getTitle());
            values.put("file_path", downloadModel.getFile_path());
            values.put("progress", downloadModel.getProgress());
            values.put("status", downloadModel.getStatus());
            values.put("file_size", downloadModel.getFile_size());
            values.put("is_paused", downloadModel.isIs_paused() ? 1 : 0);

            long result = db.insert("downloads", null, values);
            if (result == -1) {
                // Insert failed
                Log.e("DownloadDBHelper", "Failed to insert download record");
            } else {
                // Insert successful
                getAllDownload();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("DownloadDBHelper", "Exception occurred while inserting download record: " + e.getMessage());
        } finally {
            // Close the database connection
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    public void updateDownloadTitle(long id, String title){
        db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        db.update("downloads", values, "download_id=?", new String[]{String.valueOf(id)});
    }

    public void deleteDownload(long id){
        db = this.getWritableDatabase();
        db.delete("downloads","download_id=?", new String[]{String.valueOf(id)});
    }

    @SuppressLint("Range")
    public List<DownloadModel> getAllDownload() {
        db=this.getWritableDatabase();
        Cursor cursor = null;
        List<DownloadModel> downloadList = new ArrayList<>();

        db.beginTransaction();
        try {
            cursor = db.query("downloads", null, null, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        DownloadModel download = new DownloadModel();
                        download.setDownloadId(cursor.getLong(cursor.getColumnIndex("download_id")));
                        download.setTitle(cursor.getString(cursor.getColumnIndex("title")));
                        download.setFile_path(cursor.getString(cursor.getColumnIndex("file_path")));
                        download.setProgress(cursor.getString(cursor.getColumnIndex("progress")));
                        download.setStatus(cursor.getString(cursor.getColumnIndex("status")));
                        download.setFile_size(cursor.getString(cursor.getColumnIndex("file_size")));
                        download.setIs_paused(cursor.getInt(cursor.getColumnIndex("is_paused")) == 1);
                        downloadList.add(download);

                    } while (cursor.moveToNext());
                }
            }
        } finally {
            db.endTransaction();
            cursor.close();
        }
        return downloadList;
    }
}