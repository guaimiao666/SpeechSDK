package com.smarteye.speech.sdk;

import android.content.Context;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.smarteye.speech.R;

public class SpeechSDK {

    private final String TAG = "SpeechSDK";
    private static SpeechSDK instance;
    private static boolean mscInitialize = false;
    private Context mContext;

    public static SpeechSDK getInstance() {
        if (instance == null) {
            instance = new SpeechSDK();
        }
        return instance;
    }

    public void init(Context context) {
        mContext = context;
        initializeMsc(context);
        SpeechControl.getInstance().init(context);
    }

    private void initializeMsc(Context context) {
        if (mscInitialize) {
            Log.w(TAG, "Msc Already Init!");
            return;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("appid=" + context.getString(R.string.app_id));
        buffer.append(",");
        buffer.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(context, buffer.toString());
        mscInitialize = true;
        Log.i(TAG, "初始化Msc");
    }

    public void unInit() {
        SpeechControl.getInstance().unInit();
    }

    public void writeAudioData(byte[] data, int dataLength) {
        SpeechControl.getInstance().writeAudioData(data, dataLength);
    }

    public void setWakeupCallback(WakeupCallback wakeupCallback) {
        SpeechControl.getInstance().setWakeupCallback(wakeupCallback);
    }

    public void setRecognizeCallback(RecognizeCallback recognizeCallback) {
        SpeechControl.getInstance().setRecognizeCallback(recognizeCallback);
    }

}
