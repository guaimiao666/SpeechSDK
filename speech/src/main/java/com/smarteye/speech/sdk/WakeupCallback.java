package com.smarteye.speech.sdk;

public interface WakeupCallback {

    void onBeginOfSpeech();
    void onResult(String result);
    void onError(int errorCode, String errorMessage);
}
