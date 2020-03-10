package com.app.motolife.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.app.motolife.firebase.FirebaseUtils;
import com.app.motolife.model.User;
import com.bumptech.glide.Glide;
import com.example.motolife.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final int IMAGE_REQUEST = 1;

    private FirebaseUtils firebaseUtils;
    private CircleImageView image_profile;
    private TextView username;

    private Uri imageUri;
    private StorageTask uploadTask;

    private Button backButton;
    private Button logoutButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        setContentView(R.layout.activity_profile);
        firebaseUtils = FirebaseUtils.getInstance();

        image_profile = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        backButton = findViewById(R.id.back_button);
        logoutButton = findViewById(R.id.logout_button);

        StorageReference storageReference = firebaseUtils.getStorageReference("uploads");

        FirebaseAuth firebaseAuth = firebaseUtils.getFirebaseAuth();
        firebaseAuth.addAuthStateListener(authStateListener);
        FirebaseUser firebaseUser = firebaseUtils.getFirebaseUser();
        DatabaseReference reference = firebaseUtils.getDatabaseReference("users").child(firebaseUser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default"))
                    image_profile.setImageResource(R.mipmap.ic_launcher);
                else
                    Glide.with(getApplicationContext()).load(user.getImageURL()).into(image_profile);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        image_profile.setOnClickListener(click -> {
            openImage();
        });

        backButton.setOnClickListener(click -> finish());

        logoutButton.setOnClickListener(click -> {
            new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setMessage("Are you sure ?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                    })
                    .setNegativeButton(R.string.Cancel, null)
                    .show();
        });

    }

    private void openImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(
                Objects.requireNonNull(getApplicationContext()).getContentResolver().getType(uri));
    }

    private void uploadImage() {
        if (Objects.nonNull(imageUri)) {
            final StorageReference fileReference = firebaseUtils.getStorageReference(null).child(System.currentTimeMillis() + "." +
                    getFileExtension(imageUri));

            uploadTask = fileReference.putFile(imageUri);
            //noinspection unchecked
            uploadTask.continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());

                return fileReference.getDownloadUrl();
            }).addOnCompleteListener((OnCompleteListener<Uri>) onComplete -> {
                if (onComplete.isSuccessful()) {
                    Uri downloadUri = onComplete.getResult();
                    String mUri = downloadUri.toString();

                    DatabaseReference reference = firebaseUtils.getDatabaseReference("users")
                            .child(firebaseUtils.getFirebaseUser().getUid());
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("imageURL", mUri);
                    reference.updateChildren(map);
                } else {
                    Toast.makeText(getApplicationContext(), "Failed to change image", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(onFailure -> {
                Toast.makeText(getApplicationContext(), onFailure.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(getApplicationContext(), "No image selected !", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress())
                Toast.makeText(getApplicationContext(), "Upload in progress", Toast.LENGTH_SHORT).show();
            else
                uploadImage();
        }
    }

    FirebaseAuth.AuthStateListener authStateListener = firebaseAuth -> {
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finish();
        }
    };
}