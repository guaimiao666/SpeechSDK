package com.smarteye.speech.sdk;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.smarteye.speech.R;
import com.smarteye.speech.util.FucUtil;
import com.smarteye.speech.util.JsonParser;
import com.smarteye.speech.util.PlayUtil;
import com.smarteye.speech.util.XmlParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class SpeechControl implements PlayUtil.PlayComplete {
    private final String TAG = "SpeechControl";
    private static SpeechControl instance;
    private Context mContext;
    private boolean isDistinguish = false;// 识别中
    private boolean voiceStatus = false; //是否写入识别音频
    private AtomicBoolean isInit = new AtomicBoolean(false);
    private Handler mHandler = new Handler();

    public static SpeechControl getInstance() {
        if (instance == null) {
            instance = new SpeechControl();
        }
        return instance;
    }

    public void init(Context context) {
        if (isInit.get()) return;
        isInit.set(true);
        mContext = context;
        PlayUtil.setPlayComplete(this);
        initRecognizer();
        initWakeup();
        Log.i(TAG, "初始化语音唤醒/识别");
    }

    public void writeAudioData(byte[] data, int dataLength) {
        if (!voiceStatus) {
            if (mIvw != null) {
                mIvw.writeAudio(data, 0, dataLength);
            }
        } else {
            if (mAsr != null) {
                mAsr.writeAudio(data, 0, dataLength);
            }
        }
    }

    public void unInit() {
        stopWakeup();
        stopRecognize();
        unInitWakeup();
        unInitRecognize();
        Log.i(TAG, "注销语音唤醒/监听");
    }

    @Override
    public void onComplete(int resId) {
        if (resId == R.raw.start_recognize_voice) {
            mHandler.postDelayed(this::startRecognize, 500);
        }
    }

    /**------------------------------------------------语音唤醒模块------------------------------------------------------------ */
    private VoiceWakeuper mIvw;
    private final static int MAX = 3000;
    private final static int MIN = 0;
    private int curThresh = 0;
    private String threshStr = "门限值：";
    private String keep_alive = "1";
    private String ivwNetMode = "0";
    // 唤醒结果内容
    private String resultString;
    private WakeupCallback wakeupCallback;

    public void setWakeupCallback(WakeupCallback wakeupCallback) {
        this.wakeupCallback = wakeupCallback;
    }

    //初始化语音唤醒
    public void initWakeup() {
        mIvw = VoiceWakeuper.createWakeuper(mContext, null);
        // 清空参数
        mIvw.setParameter(SpeechConstant.PARAMS, null);
        // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
        mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
        // 设置唤醒模式
        mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
        // 设置持续进行唤醒
        mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
        // 设置闭环优化网络模式
        mIvw.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
        // 设置唤醒资源路径
        mIvw.setParameter(SpeechConstant.IVW_RES_PATH, getResource());
        // 设置唤醒录音保存路径，保存最近一分钟的音频
        mIvw.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
        mIvw.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
        //mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );
        // 启动唤醒
        /*	mIvw.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");*/
        startWakeup();
    }

    private String getResource() {
        return ResourceUtil.generateResourcePath(mContext, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + mContext.getString(R.string.app_id) + ".jet");
    }

    private WakeuperListener mWakeuperListener = new WakeuperListener() {
        @Override
        public void onBeginOfSpeech() {
            Log.i(TAG, "语音唤醒开始");
            if (wakeupCallback != null) wakeupCallback.onBeginOfSpeech();
        }

        @Override
        public void onResult(WakeuperResult wakeuperResult) {
            JSONObject object = null;
            try {
                String text = wakeuperResult.getResultString();
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 " + text);
                buffer.append("\n");
                buffer.append("【操作类型】" + object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】" + object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                resultString = buffer.toString();
            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            stopWakeup();
            Log.i(TAG, "语音唤醒结果:" + resultString);
            String score = object.optString("score");
            if (isDistinguish) {
                Log.w(TAG, "识别中......");
                return;
            }
            if (resultString.equals("结果解析出错")) {
                Log.e(TAG, "语音唤醒结果解析出错");
                return;
            }
            if (!TextUtils.isEmpty(score)) {
                int scoreValue = Integer.parseInt(score);
                if (scoreValue <= 1400) {
                    Log.w(TAG, "语音唤醒得分小于1400");
                    return;
                }
            }
            if (!setRecognizeParam()) {
                Log.w(TAG, "请先构建语法");
                return;
            }
            isDistinguish = true;
            PlayUtil.play(mContext, R.raw.start_recognize_voice);
            if (wakeupCallback != null) wakeupCallback.onResult(resultString);
        }

        @Override
        public void onError(SpeechError speechError) {
            Log.e(TAG, "语音唤醒出错! 错误码:" + speechError.getErrorCode());
        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {

        }

        @Override
        public void onVolumeChanged(int i) {

        }
    };

    private void startWakeup() {
        if (mIvw != null) {
            mIvw.startListening(mWakeuperListener);
            Log.i(TAG, "开始语音唤醒监听");
        }
    }

    private void stopWakeup() {
        if (mIvw != null) {
            mIvw.stopListening();
            Log.i(TAG, "停止语音唤醒监听");
        }
    }

    private void unInitWakeup() {
        if (mIvw != null) {
            mIvw.cancel();
            mIvw.destroy();
            Log.i(TAG, "销毁语音唤醒");
        }
    }

    /**------------------------------------------------离线命令词识别------------------------------------------------------------ */
    private SpeechRecognizer mAsr;
    private String mEngineType = SpeechConstant.TYPE_LOCAL;
    private String mResultType = "json";
    private String grmPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/msc/test";
    private String mLocalGrammar = null;
    private final String GRAMMAR_TYPE_BNF = "bnf";
    private int ret;
    private RecognizeCallback recognizeCallback;

    public void setRecognizeCallback(RecognizeCallback recognizeCallback) {
        this.recognizeCallback = recognizeCallback;
    }

    //初始化语音识别
    private void initRecognizer() {
        // 初始化识别对象
        mAsr = SpeechRecognizer.createRecognizer(mContext, mInitListener);
        mLocalGrammar = FucUtil.readFile(mContext, "call.bnf", "utf-8");
        // 本地-构建语法文件，生成语法id
        mAsr.setParameter(SpeechConstant.PARAMS, null);
        // 设置文本编码格式
        mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        // 设置引擎类型
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        Log.i(TAG, "语音识别设置引擎 成功!= " + mEngineType);
        // 设置语法构建路径
        mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        Log.i(TAG, "语音识别设置语法构建路径 成功!= " + grmPath);
        //使用8k音频的时候请解开注释
//            mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
        // 设置资源路径
        mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        //只有设置这个属性为1时,VAD_BOS  VAD_EOS才会生效,且RecognizerListener.onVolumeChanged才有音量返回默认：1
        mAsr.setParameter(SpeechConstant.VAD_ENABLE, "1");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mAsr.setParameter(SpeechConstant.VAD_BOS, "10000");
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mAsr.setParameter(SpeechConstant.VAD_EOS, "10000");
        ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mLocalGrammar, grammarListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.e(TAG, "语音识别语法构建失败,错误码：" + ret);
        }
        mAsr.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = code -> {
        Log.d(TAG, "SpeechRecognizer init code = " + code);
        if (code != ErrorCode.SUCCESS) {
            Log.w(TAG, "语音识别初始化失败,错误码：\" + code + \",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    };

    /**
     * 构建语法监听器。
     */
    private GrammarListener grammarListener = (grammarId, error) -> { //返回构建语法结果
        if (error == null) {
            Log.i(TAG, "语音识别语法构建成功：" + grammarId);
        } else {
            Log.e(TAG, "语音识别语法构建失败,错误码：" + error.getErrorCode());
        }
    };

    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {

        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            String text = "";
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                if (mResultType.equals("json")) {
                    text = JsonParser.parseGrammarResult(result.getResultString(), mEngineType);
                } else if (mResultType.equals("xml")) {
                    text = XmlParser.parseNluResult(result.getResultString());
                } else {
                    text = result.getResultString();
                }
            } else {
                Log.d(TAG, "语音识别结果为空");
            }
            Log.d(TAG, "语音识别结果:" + text);
            isDistinguish = false;
            if (TextUtils.isEmpty(text)) return;
            if (text.contains("置信度")) {
                String string = text.substring(text.lastIndexOf("置信度") + 4, text.length() - 1);
                try {
                    if (Integer.parseInt(string) > 50) {//置信度大于50
                        if (recognizeCallback != null) recognizeCallback.onResult(text);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onEndOfSpeech() {
            Log.i(TAG, "语音识别结束");
            if (recognizeCallback != null) recognizeCallback.onEndOfSpeech();
            PlayUtil.play(mContext, R.raw.end_recognize_voice);
            stopRecognize();
            startWakeup();
        }

        @Override
        public void onBeginOfSpeech() {
            Log.i(TAG, "语音识别开始");
            if (recognizeCallback != null) recognizeCallback.onBeginOfSpeech();
        }

        @Override
        public void onError(SpeechError error) {
            Log.i(TAG, "语音识别出错 错误码:" + error.getErrorCode());
            if (recognizeCallback != null)
                recognizeCallback.onError(error.getErrorCode(), error.getErrorDescription());
            isDistinguish = false;
            stopRecognize();
            startWakeup();
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }

    };

    /**
     * 参数设置
     *
     * @return
     */
    public boolean setRecognizeParam() {
        boolean result = false;
        // 清空参数
        mAsr.setParameter(SpeechConstant.PARAMS, null);
        // 设置识别引擎
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mAsr.setParameter(SpeechConstant.ASR_PTT, "0");
        // 设置本地识别资源
        mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 设置语法构建路径
        mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置返回结果格式
        mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
        // 设置本地识别使用语法id
        mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "call");
        // 设置识别的门限值
        mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
        // 使用8k音频的时候请解开注释
//			mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
        result = true;
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mAsr.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mAsr.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/asr.wav");
        return result;
    }

    //获取识别资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(mContext, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        return tempBuffer.toString();
    }

    private void startRecognize() {
        if (mAsr != null) {
            mAsr.startListening(mRecognizerListener);
            voiceStatus = true;
            Log.i(TAG, "开始语音识别监听");
        }
    }

    private void stopRecognize() {
        if (mAsr != null) {
            mAsr.stopListening();
            voiceStatus = false;
            Log.i(TAG, "停止语音识别监听");
        }
    }

    private void unInitRecognize() {
        if (mAsr != null) {
            mAsr.cancel();
            mAsr.destroy();
            Log.i(TAG, "销毁语音识别");
        }
    }

}
