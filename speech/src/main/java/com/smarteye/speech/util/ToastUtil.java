package com.smarteye.speech.util;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    public static void showMessage(Context context, String message, boolean isLong) {
        Toast.makeText(context, message, isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }
}
