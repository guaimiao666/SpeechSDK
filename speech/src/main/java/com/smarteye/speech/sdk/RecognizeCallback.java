package com.smarteye.speech.sdk;

public interface RecognizeCallback {
    void onBeginOfSpeech();
    void onEndOfSpeech();
    void onResult(String result);
    void onError(int errorCode, String errorMessage);
}
