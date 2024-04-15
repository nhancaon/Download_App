package com.example.dowloadfile.Fragment;
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
import com.google.android.gms.tasks.Task;

import com.example.dowloadfile.Adapter.GridViewAdapter;
import com.example.dowloadfile.Model.DownloadModel;
import com.example.dowloadfile.R;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * A simple {@link Fragment} subclass.
 */
public class StorageFirebaseFragment extends Fragment {
    private GridView gridView;
    private ArrayList<DownloadModel> dataList;
    private GridViewAdapter adapter;

    public StorageFirebaseFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_firebase_storage, container, false);
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
