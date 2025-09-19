# consumer-rules.pro
-keep class com.iflytek.** { *; }
-keep class com.smarteye.speech.** { *; }

# 保持公共API不被混淆
-keep public class com.smarteye.speech.util.PlayUtil { *; }