package com.smarteye.speech.util;

import android.content.Context;
import android.media.MediaPlayer;

public class PlayUtil {
    private static PlayComplete playComplete;
    private static int currentResId;

    public static void setPlayComplete(PlayComplete playComplete) {
        PlayUtil.playComplete = playComplete;
    }

    public static void play(Context context, int resId) {
        currentResId = resId;
        MediaPlayer mediaPlayer = MediaPlayer.create(context, resId);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
            if (playComplete != null) {
                playComplete.onComplete(currentResId);
            }
        });
        mediaPlayer.start();
    }

    public interface PlayComplete {
        void onComplete(int resId);
    }
}
