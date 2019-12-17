package com.app.motolife.ui;

import android.app.Activity;
import android.media.MediaPlayer;

import com.example.motolife.R;

public class SoundService {

    private static MediaPlayer mediaPlayer;
    private static Activity activity;

    public SoundService(Activity activity) {
        SoundService.activity = activity;
    }

    public void makePokeSound() {
        mediaPlayer = MediaPlayer.create(activity.getApplicationContext(), R.raw.poke);
        mediaPlayer.start();
    }
}
