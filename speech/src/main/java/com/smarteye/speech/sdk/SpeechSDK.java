package com.smarteye.speech.sdk;

public class SpeechSDK {

    private final String TAG = "SpeechSDK";
    private static SpeechControl speechControl;

    public static SpeechControl getSDK() {
        if (speechControl == null) {
            speechControl = new SpeechControl();
        }
        return speechControl;
    }
}
