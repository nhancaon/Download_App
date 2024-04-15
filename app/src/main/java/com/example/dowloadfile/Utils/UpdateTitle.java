package com.example.dowloadfile.Utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dowloadfile.Model.DownloadModel;
import com.example.dowloadfile.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.util.List;

public class UpdateTitle extends BottomSheetDialogFragment {
    public interface OnTitleUpdateListener {
        void onTitleUpdated(String updatedTitle);
    }

    public static final String TAG = "UpdateTitle";
    private EditText mEditText;
    private Button mSaveButton;
    String titleIncludeFileType;
    private OnTitleUpdateListener mListener;
    private static String changedTitle = "";
    private DownloadDBHelper myDB;

    public static UpdateTitle newInstance() {
        return new UpdateTitle();
    }

    public void setOnTitleUpdateListener(OnTitleUpdateListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_rename_title, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEditText = view.findViewById(R.id.edtText);
        mSaveButton = view.findViewById(R.id.btnSave);
        myDB = new DownloadDBHelper(requireActivity());

        boolean isUpdate = false;
        Bundle bundle = getArguments();

        if (bundle != null){
            isUpdate = true;
            titleIncludeFileType = bundle.getString("titleIncludeFileType");
        }
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals("")){
                    mSaveButton.setEnabled(false);
                    mSaveButton.setBackgroundColor(Color.GRAY);
                }else{
                    mSaveButton.setEnabled(true);
                    mSaveButton.setBackgroundColor(getResources().getColor(R.color.primaryColor));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        final boolean finalIsUpdate = isUpdate;
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changedTitle = mEditText.getText().toString() + getFileType(titleIncludeFileType);

                if(finalIsUpdate){
                    // Notify the listener with the updated title
                    if (mListener != null) {
                        mListener.onTitleUpdated(changedTitle);
                    }
                    myDB.updateDownloadTitle(bundle.getLong("download_id"), changedTitle);
                    Toast.makeText(requireContext(), "Title changed", Toast.LENGTH_LONG).show();
                }
                else{
                    DownloadModel item = new DownloadModel();
                    item.setTitle(changedTitle);
                    myDB.insertDownload(item);
                    Toast.makeText(requireContext(), "Title added", Toast.LENGTH_LONG).show();
                }
                dismiss();
            }
        });
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if(activity instanceof OnDialogCloseListener){
            ((OnDialogCloseListener)activity).onDialogClose(dialog);
        }
    }


    public String getFileType(String fileName) {
        if (fileName != null && fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0) {
            return "." + fileName.substring(fileName.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }

}
