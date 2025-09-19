package com.smarteye.speech.sdk;

public interface WakeupCallback {

    void wakeupBegin();
    void wakeupResult(String result);
    void wakeupError(int errorCode, String errorMessage);
}
