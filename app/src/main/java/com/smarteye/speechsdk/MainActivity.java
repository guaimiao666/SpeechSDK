package com.smarteye.speechsdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smarteye.speech.sdk.RecognizeCallback;
import com.smarteye.speech.sdk.SpeechSDK;
import com.smarteye.speech.sdk.WakeupCallback;
import com.smarteye.speechsdk.databinding.ActivityMainBinding;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity implements View.OnClickListener, WakeupCallback, RecognizeCallback {

    private final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private final String[] permissions = {
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private AudioRecord audioRecord;
    private AudioRecordThread audioRecordThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        if (checkPermissions()) {
            init();
        } else {
            requestPermissions();
        }
    }

    private boolean checkPermissions() {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, permissions, 100);
    }

    private void init() {
        binding.button.setOnClickListener(this);
        binding.button2.setOnClickListener(this);
        binding.button3.setOnClickListener(this);
        binding.button4.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && requestCode == 100) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    finish();
                    return;
                }
            }
            init();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.button) {
            SpeechSDK.getSDK().init(this);
            SpeechSDK.getSDK().setWakeupCallback(this);
            SpeechSDK.getSDK().setRecognizeCallback(this);
        } else if (id == R.id.button2) {
            SpeechSDK.getSDK().unInit();
        } else if (id == R.id.button3) {
            startAudio();
        } else if (id == R.id.button4) {
            stopAudio();
        }
    }

    @Override
    public void wakeupBegin() {
        Log.i(TAG, "wakeupBegin");
    }

    @Override
    public void wakeupResult(String result) {
        Log.i(TAG, "wakeupResult result:" + result);
    }

    @Override
    public void wakeupError(int errorCode, String errorMessage) {
        Log.i(TAG, "wakeupError errorCode:" + errorCode + " errorMessage:" + errorMessage);
    }

    @Override
    public void recognizeBegin() {
        Log.i(TAG, "recognizeBegin");
    }

    @Override
    public void recognizeEnd() {
        Log.i(TAG, "recognizeEnd");
    }

    @Override
    public void recognizeResult(String result) {
        Log.i(TAG, "recognizeResult result:" + result);
    }

    @Override
    public void recognizeError(int errorCode, String errorMessage) {
        Log.i(TAG, "recognizeError errorCode:" + errorCode + " errorMessage:" + errorMessage);
    }


    int minBufferSize;
    boolean isOpenAudioRecordFlag = false;
    private void startAudio() {
        minBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, 16000,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        if (!isOpenAudioRecordFlag) {
            isOpenAudioRecordFlag = true;
            if (audioRecordThread == null) {
                audioRecordThread = new AudioRecordThread();
                audioRecordThread.start();
            }
        }
    }

    private void stopAudio() {
        if (isOpenAudioRecordFlag) {
            isOpenAudioRecordFlag = false;
            if (audioRecordThread != null) {
                audioRecordThread.interrupt();
                audioRecordThread = null;
            }
        }
    }

    private class AudioRecordThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (isOpenAudioRecordFlag) {
                try {
                    byte[] buffer = new byte[minBufferSize];
                    audioRecord.read(buffer, 0, minBufferSize);
                    SpeechSDK.getSDK().writeAudioData(buffer, buffer.length);
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "audio record thread error:" + e.getMessage());
                }
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        SpeechSDK.getSDK().unInit();
        stopAudio();
    }
}