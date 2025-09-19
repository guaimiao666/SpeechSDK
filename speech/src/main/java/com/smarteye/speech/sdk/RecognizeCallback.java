package com.smarteye.speech.sdk;

public interface RecognizeCallback {
    void recognizeBegin();
    void recognizeEnd();
    void recognizeResult(String result);
    void recognizeError(int errorCode, String errorMessage);
}
