package com.example.motolife.URI;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

public class PowerOffController {


    private PowerOffController() {
    }

    public static void powerOff(final boolean[] flag, Activity activity) {
        if (flag[0]) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity.getApplicationContext(), "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show();
            flag[0] = true;
            new Handler().postDelayed(() -> flag[0] = false, 2000);
        }
    }
}
