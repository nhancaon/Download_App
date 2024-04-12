package com.example.dowloadfile;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * A simple {@link Fragment} subclass.
 */
public class DoneFragment extends Fragment {
    private GridView gridView;
    private ArrayList<DownloadModel> dataList;
    private GridViewAdapter adapter;

    public DoneFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_done, container, false);
        gridView = view.findViewById(R.id.gridView);
        dataList = new ArrayList<>();
        adapter = new GridViewAdapter(requireContext(), dataList);
        gridView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseStorage.getInstance().getReference().child("image").listAll().addOnCompleteListener(new OnCompleteListener<ListResult>() {
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
}

