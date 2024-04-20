package com.example.dowloadfile.Fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.dowloadfile.Adapter.ImageAdapter;
import com.example.dowloadfile.Model.DataModel;
import com.example.dowloadfile.Model.DownloadModel;
import com.example.dowloadfile.R;


import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UploadFragment extends Fragment {
    private static final int PICK_IMAGES_REQUEST_CODE = 123;
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private FloatingActionButton uploadButton;
    private List<Uri> imageUris = new ArrayList<>();
    EditText uploadCaption;
    ProgressBar progressBar;
    private ImageView uploadImage;
    private Uri imageUri;
    final  private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Images");
    final private StorageReference storageReference = FirebaseStorage.getInstance().getReference();

    public UploadFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_upload, container, false);
        recyclerView = view.findViewById(R.id.imageRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        uploadButton = view.findViewById(R.id.uploadButton);
        uploadCaption = view.findViewById(R.id.uploadCaption);
        uploadImage = view.findViewById(R.id.uploadImage);
        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Intent data = result.getData();
                            if (data.getClipData() != null) {
                                int count = data.getClipData().getItemCount();
                                for (int i = 0; i < count; i++) {
                                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                    imageUris.add(imageUri);
                                }
                            } else if (data.getData() != null) {
                                Uri imageUri = data.getData();
                                imageUris.add(imageUri);
                            }
                            showRecyclerViewIfImagesSelected();
                        } else {
                            Toast.makeText(requireActivity(), "No Images Selected", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoPicker = new Intent();
                photoPicker.setAction(Intent.ACTION_GET_CONTENT);
                photoPicker.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple image selection
                photoPicker.setType("image/*");
                activityResultLauncher.launch(photoPicker);
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!imageUris.isEmpty()) {
                    for (Uri uri : imageUris) {
                        uploadToFirebase(uri);
                    }
                } else {
                    Toast.makeText(requireActivity(), "Please select at least one image", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }

    private void uploadToFirebase(Uri uri) {
        String caption = uploadCaption.getText().toString();
        final StorageReference imageReference = storageReference
                .child(System.currentTimeMillis() + "." + getFileExtension(Uri.parse(String.valueOf(uri))));

        imageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                imageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        progressBar.setVisibility(View.GONE);
                        DataModel dataClass = new DataModel(uri.toString(), caption);
                        String key = databaseReference.push().getKey();
                        databaseReference.child(key).setValue(dataClass);
                        Toast.makeText(requireActivity(), "Firebase uploaded", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed", Toast.LENGTH_SHORT).show();
            }
        });
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
    private void showRecyclerViewIfImagesSelected() {
        if (!imageUris.isEmpty()) {
            recyclerView.setVisibility(View.VISIBLE);
            imageAdapter = new ImageAdapter(requireContext(), imageUris);
            recyclerView.setAdapter(imageAdapter);
        }
    }
}