package com.app.motolife.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

import javax.annotation.Nullable;

public class FirebaseUtils {

    private static FirebaseUtils firebaseUtils;

    private FirebaseUtils() {
    }

    public static FirebaseUtils getInstance() {
        if (Objects.isNull(firebaseUtils))
            firebaseUtils = new FirebaseUtils();
        return firebaseUtils;
    }

    public FirebaseAuth getFirebaseAuth() {
        return FirebaseAuth.getInstance();
    }

    public FirebaseUser getFirebaseUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public DatabaseReference getDatabaseReference(@Nullable String url) {
        return Objects.isNull(url)
                ? FirebaseDatabase.getInstance().getReference()
                : FirebaseDatabase.getInstance().getReference(url);
    }

    public StorageReference getStorageReference(@Nullable String url) {
        return Objects.isNull(url)
                ? FirebaseStorage.getInstance().getReference()
                : FirebaseStorage.getInstance().getReference(url);
    }
}
