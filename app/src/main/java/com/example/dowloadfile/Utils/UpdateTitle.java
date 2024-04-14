package com.example.dowloadfile.Utils;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Color;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.dowloadfile.Fragment.AddFragment;
import com.example.dowloadfile.Model.DownloadModel;
import com.example.dowloadfile.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class UpdateTitle extends BottomSheetDialogFragment {
    public static final String TAG = "UpdateTitle";
    private EditText mEditText;
    private Button mSaveButton;
    String titleIncludeFileType;
    private DownloadDBHelper myDB;
    public static UpdateTitle newInstance(){
        return new UpdateTitle();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_rename_title, container, false);
        return v;
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
                String text = mEditText.getText().toString() + getFileType(titleIncludeFileType);

                if(finalIsUpdate){
                    myDB.updateDownloadTitle(bundle.getLong("download_id"), text);
                    Toast.makeText(requireContext(), "Title changed", Toast.LENGTH_LONG).show();
                }
                else{
                    DownloadModel item = new DownloadModel();
                    item.setTitle(text);
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
