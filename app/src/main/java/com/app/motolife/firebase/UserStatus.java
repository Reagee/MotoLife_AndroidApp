package com.app.motolife.firebase;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

public class UserStatus {

    public static void ONLINE(){
        setStatus("online");
    }

    public static void OFFLINE(){
        setStatus("offline");
    }

    private static void setStatus(String status){
        FirebaseUtils firebaseUtils = FirebaseUtils.getInstance();
        FirebaseUser firebaseUser = firebaseUtils.getFirebaseUser();
        DatabaseReference reference = firebaseUtils.getDatabaseReference("users").child(firebaseUser.getUid());

        HashMap<String, Object> map = new HashMap<>();
        map.put("status", status);

        reference.updateChildren(map);
    }

}
