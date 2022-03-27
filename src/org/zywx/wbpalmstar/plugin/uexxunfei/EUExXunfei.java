package org.zywx.wbpalmstar.plugin.uexxunfei;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.base.BDebug;
import org.zywx.wbpalmstar.base.BUtility;
import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserActivity;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.engine.universalex.EUExCallback;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitInputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitOutputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitRecognizerInputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitRecognizerOutputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitSpeakerInputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitSpeakerOutputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.RecognizeErrorVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.StartSpeakingVO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class EUExXunfei extends EUExBase {
    private static final String TAG = "EUExXunfei";

    private static final String BUNDLE_DATA = "data";
    private static final int REQUEST_CODE_RECORD_AUDIO_PERMISSION = 2000;

    private SpeechSynthesizer mTts = null;
    private SpeechRecognizer mIat = null;

    private static final int MSG_INIT = 1;
    private static final int MSG_INIT_SPEAKER = 2;

    private String mCallbackWinName = "root";
    private String[] mInitParams;
    private int mInitCallbackId = -1;
    EBrowserView eBrowserView;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            Log.e("TAG", "==============onVolumeChanged");
        }

        @Override
        public void onBeginOfSpeech() {
            Log.e("TAG", "==============onBeginOfSpeech");

        }

        @Override
        public void onEndOfSpeech() {
            Log.e("TAG", "==============onEndOfSpeech");
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean b) {
            Log.e("TAG", "==============onResult" + recognizerResult.getResultString());
            printResult(recognizerResult);
        }

        @Override
        public void onError(SpeechError speechError) {
            Log.e("TAG", "==============onError");
            RecognizeErrorVO errorVO = new RecognizeErrorVO();
            errorVO.error = speechError.getErrorDescription();
            callBackPluginJs(JsConst.ON_RECOGNIZE_ERROR, DataHelper.gson.toJson(errorVO));

        }

        @Override
        public void onEvent(int i, int i1, int i2, Bundle bundle) {
            Log.e("TAG", "==============onEvent");

        }
    };

    public EUExXunfei(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
        this.eBrowserView = eBrowserView;
    }

    @Override
    protected boolean clean() {
        return false;
    }


    public void init(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
        }
        //4.0回调参数返回，与ios保持一致, ios插件中不能使用init方法
        mInitCallbackId = -1;
        mInitParams = params;
        if (params.length == 2) {
            try {
                mInitCallbackId = Integer.parseInt(params[1]);
            } catch (Exception e) {
            }
        }
        mCallbackWinName = mBrwView.getWindowName();
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(Manifest.permission.RECORD_AUDIO, "请先申请权限"
                    + Manifest.permission.RECORD_AUDIO, REQUEST_CODE_RECORD_AUDIO_PERMISSION);
        } else {
            initMsg(params);
        }
    }

    private void initMsg(String[] params) {
        String json = params[0];
        InitInputVO initInputVO = DataHelper.gson.fromJson(json, InitInputVO.class);
        SpeechUtility speechUtility = SpeechUtility.createUtility(mContext.getApplicationContext(), SpeechConstant
                .APPID + "=" +
                initInputVO.appID);
        InitOutputVO outputVO = new InitOutputVO();
        outputVO.result = (speechUtility != null);
        if (mInitCallbackId != -1) {
            callbackToJs(mInitCallbackId, false, outputVO.result ? EUExCallback.F_C_SUCCESS : EUExCallback.F_C_FAILED);
        } else {
            callBackPluginJs(JsConst.CALLBACK_INIT, DataHelper.gson.toJson(outputVO));
        }
    }

    private void callbackPermissionDenied(String[] permissions) {
        // 定位权限请求失败，需要给前端回调这种情况
        JSONObject resultJson = null;
        int errorCode = 2;
        try {
            resultJson = new JSONObject();
            if (permissions == null || permissions.length == 0) {
                resultJson.put("errCode", errorCode);
                resultJson.put("errMsg", "未授权录音权限，录音功能将无法使用");
            } else {
                BDebug.i(TAG, "onRequestPermissionResult: request denied: ", permissions[0]);
                // 对于 ActivityCompat.shouldShowRequestPermissionRationale
                // 1：用户拒绝了该权限，没有勾选"不再提醒"，此方法将返回true。
                // 2：用户拒绝了该权限，有勾选"不再提醒"，此方法将返回 false。
                // 3：如果用户同意了权限，此方法返回false
                // 拒绝了权限且勾选了"不再提醒"
                // 总之：此方法返回false的时候，代表用户再也不想要这个权限了，也没法申请了。只能再次弹提示说明丢失权限的后果，功能无法使用。
                if (ActivityCompat.shouldShowRequestPermissionRationale((EBrowserActivity)mContext, permissions[0])) {
                    errorCode = 201;
                    resultJson.put("errCode", errorCode);
                    resultJson.put("errMsg", "本次录音权限请求失败，无法使用语音识别等功能：" + permissions[0]);
                } else {
                    errorCode = 202;
                    resultJson.put("errCode", errorCode);
                    resultJson.put("errMsg", "录音权限无法获取，语音识别等功能将无法使用：" + permissions[0]);
                }
            }
        } catch (Exception e) {
            if (BDebug.isDebugMode()){
                e.printStackTrace();
            }
        }
        InitOutputVO outputVO = new InitOutputVO();
        outputVO.result = false;
        if (mInitCallbackId != -1) {
            callbackToJs(mInitCallbackId, false, errorCode, resultJson);
        } else {
            callBackPluginJs(JsConst.CALLBACK_INIT, DataHelper.gson.toJson(outputVO));
        }
        BDebug.i(TAG, "onRequestPermissionResult callback: ", resultJson);
    }

    public void initSpeaker(String[] params) {
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_INIT_SPEAKER;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void initSpeakerMsg(String[] params) {
        String json;
        if (params.length == 0) {
            json = "{}";
        } else {
            json = params[0];
        }
        InitSpeakerInputVO inputVO = DataHelper.gson.fromJson(json, InitSpeakerInputVO.class);
        if (mTts == null) {
            mTts = SpeechSynthesizer.createSynthesizer(mContext.getApplicationContext(), new InitListener() {
                @Override
                public void onInit(int i) {
                    InitSpeakerOutputVO outputVO = new InitSpeakerOutputVO();
                    outputVO.result = (i == 0);
                    outputVO.resultCode = i;
                    callBackPluginJs(JsConst.CALLBACK_INIT_SPEAKER, DataHelper.gson.toJson(outputVO));
                }
            });
        }
        mTts.setParameter(SpeechConstant.VOICE_NAME, inputVO.voiceName);//设置发音人
        mTts.setParameter(SpeechConstant.SPEED, inputVO.speed);//设置语速
        mTts.setParameter(SpeechConstant.VOLUME, inputVO.volume);//设置音量，范围0~100
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置本地
        //设置合成音频保存位置（可自定义保存位置），保存在“./sdcard/iflytek.pcm”
        //保存在SD卡需要在AndroidManifest.xml添加写SD卡权限
        //如果不需要保存合成音频，注释该行代码
        //mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, "./sdcard/iflytek.pcm");

    }

    @Override
    public void onHandleMessage(Message message) {
        if (message == null) {
            return;
        }
        Bundle bundle = message.getData();
        switch (message.what) {
            case MSG_INIT:
                initMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            case MSG_INIT_SPEAKER:
                initSpeakerMsg(bundle.getStringArray(BUNDLE_DATA));
                break;
            default:
                super.onHandleMessage(message);
        }
    }

    /**
     * 语音合成
     *
     * @param params
     */
    public void startSpeaking(String[] params) {
        if (mTts == null) {
            return;
        }
        String json = params[0];
        StartSpeakingVO speakingVO = DataHelper.gson.fromJson(json, StartSpeakingVO.class);
        mTts.startSpeaking(speakingVO.text, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                callBackPluginJs(JsConst.ON_SPEAK_BEGIN, "");
            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {
                callBackPluginJs(JsConst.ON_SPEAK_PAUSED, "");
            }

            @Override
            public void onSpeakResumed() {
                callBackPluginJs(JsConst.ON_SPEAK_RESUMED, "");
            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {
                callBackPluginJs(JsConst.ON_SPEAK_COMPLETE, "");
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
    }

    public void initRecognizer(String[] params) {
        String json = params[0];
        InitRecognizerInputVO inputVO = DataHelper.gson.fromJson(json, InitRecognizerInputVO.class);
        if (mIat == null) {
            mIat = SpeechRecognizer.createRecognizer(mContext.getApplicationContext(), null);
        }
        String domain = "iat";
        String language = "zh_cn";
        String accent = "mandarin";
        if (!TextUtils.isEmpty(inputVO.domain)) {
            domain = inputVO.domain;
        }
        if (!TextUtils.isEmpty(inputVO.language)) {
            language = inputVO.language;
        }
        if (!TextUtils.isEmpty(inputVO.accent)) {
            accent = inputVO.accent;
        }
        mIat.setParameter(SpeechConstant.DOMAIN, domain);
        mIat.setParameter(SpeechConstant.LANGUAGE, language);
        mIat.setParameter(SpeechConstant.ACCENT, accent);

        InitRecognizerOutputVO outputVO = new InitRecognizerOutputVO();
        outputVO.result = true;
        callBackPluginJs(JsConst.CALLBACK_INIT_RECOGNIZER, DataHelper.gson.toJson(outputVO));
    }

    private InputStream ins;

    /**
     * 读取本地音频信息
     *
     * @param param
     */
    public void readLocalSouce(String[] param) {
        if (param.length < 1 || param[0] == null || mIat == null) {
            return;
        }
        String vedioPath="";
        try {
            JSONObject json=new JSONObject(param[0]);
             vedioPath = json.optString("filePath");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(null==vedioPath||"".equals(vedioPath))
            return;
//        String vedioPath = param[0];
        String realVedioPath = BUtility.makeRealPath(vedioPath, eBrowserView.getCurrentWidget().m_widgetPath, eBrowserView.getCurrentWidget().m_wgtType);
        if (vedioPath.startsWith(BUtility.F_Widget_RES_SCHEMA)) {
            try {
                ins = mContext.getAssets().open(realVedioPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            try {
                ins=new FileInputStream(new File(realVedioPath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");
        int ret = mIat.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.e("TAG", "识别失败,错误码：" + ret);
        } else {
            try {
//                ins = mContext.getAssets().open(realVedioPath);
                byte[] data = new byte[ins.available()];
                ins.read(data);
                ins.close();
                if (null != data) {
                    // 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），
                    // 位长16bit，单声道的wav或者pcm
                    // 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
                    // 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别。
                    // 音频切分方法：FucUtil.splitBuffer(byte[] buffer,int length,int spsize);
                    mIat.writeAudio(data, 0, data.length);
                    mIat.stopListening();
                } else {
                    mIat.cancel();
                    Log.e("TAG", "读取音频流失败");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    public void startListening(String[] params) {
        if (mIat == null) {
            return;
        }
        mIat.startListening(new RecognizerListener() {
            @Override
            public void onVolumeChanged(int i, byte[] bytes) {
                System.out.println("volume:" + i);
            }

            @Override
            public void onBeginOfSpeech() {
                callBackPluginJs(JsConst.ON_BEGIN_OF_SPEECH, "");
            }

            @Override
            public void onEndOfSpeech() {
                callBackPluginJs(JsConst.ON_END_OF_SPEECH, "");
            }

            //听写结果回调接口(返回Json格式结果，用户可参见附录)；
            //一般情况下会通过onResults接口多次返回结果，完整的识别内容是多次结果的累加；
            //关于解析Json的代码可参见MscDemo中JsonParser类；
            //isLast等于true时会话结束。
            @Override
            public void onResult(RecognizerResult recognizerResult, boolean isLast) {
                callBackPluginJs(JsConst.ON_RECOGNIZE_RESULT, recognizerResult.getResultString());
            }

            @Override
            public void onError(SpeechError speechError) {
                RecognizeErrorVO errorVO = new RecognizeErrorVO();
                errorVO.error = speechError.getErrorDescription();
                callBackPluginJs(JsConst.ON_RECOGNIZE_ERROR, DataHelper.gson.toJson(errorVO));
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });
//        mIat.stopListening();
//        mIat.cancel();
    }

    public void stopSpeaking(String[] params) {
        if (mTts != null) {
            mTts.stopSpeaking();
        }
    }

    public void pauseSpeaking(String[] params) {
        if (mTts != null) {
            mTts.pauseSpeaking();
        }
    }

    public void resumeSpeaking(String[] params) {
        if (mTts != null) {
            mTts.resumeSpeaking();
        }
    }

    public void stopListening(String[] params) {
        if (mIat != null) {
            mIat.stopListening();
        }
    }

    public void cancelListening(String[] params) {
        if (mIat != null) {
            mIat.cancel();
        }
    }

    public void initWakeuper(String[] params) {
        String jetPath = null;
        if (params.length < 1) {
            BDebug.w(TAG, "initWakeuper failed: params.length < 1");
            return;
        }
        // 初始化唤醒对象
        VoiceWakeuper voiceWakeuper = VoiceWakeuper.createWakeuper(mContext, new InitListener() {
            @Override
            public void onInit(int i) {
                BDebug.i(TAG, "onInit:" + i);
            }
        });
        try {
            // 清空参数
//            voiceWakeuper.setParameter(SpeechConstant.PARAMS, null);
            // 开始设置参数
            JSONObject json = new JSONObject(params[0]);
            jetPath = json.getString("wakupEnginPath");
            String jetNativePath = BUtility.makeRealPath(jetPath, mBrwView);
            if (!TextUtils.isEmpty(jetNativePath) && jetNativePath.startsWith("widget/")) {
                jetNativePath = ResourceUtil.generateResourcePath(mContext, ResourceUtil.RESOURCE_TYPE.assets, jetNativePath);
            }
            BDebug.i(TAG, "initWakeuper jetNativePath: " + jetNativePath);
            // 设置唤醒资源路径
            voiceWakeuper.setParameter(SpeechConstant.IVW_RES_PATH, jetNativePath);

            final int curThresh = 1450;
            // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
            voiceWakeuper.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
            // 设置唤醒模式
            voiceWakeuper.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 设置持续进行唤醒
            voiceWakeuper.setParameter(SpeechConstant.KEEP_ALIVE, "0");
            // 设置闭环优化网络模式
            voiceWakeuper.setParameter(SpeechConstant.IVW_NET_MODE, "0");
            // 设置唤醒录音保存路径，保存最近一分钟的音频
//            voiceWakeuper.setParameter(SpeechConstant.IVW_AUDIO_PATH,
//                    getExternalFilesDir("msc").getAbsolutePath() + "/ivw.wav");
//            voiceWakeuper.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
            //voiceWakeuper.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );
            // 启动唤醒
            /*	voiceWakeuper.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");*/

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void startWakeuper(String[] params) {
        VoiceWakeuper voiceWakeuper = VoiceWakeuper.getWakeuper();
        if (voiceWakeuper != null) {

            voiceWakeuper.startListening(new WakeuperListener() {
                @Override
                public void onBeginOfSpeech() {
                    BDebug.i(TAG, "WakeuperListener", "onBeginOfSpeech");
                }

                @Override
                public void onResult(WakeuperResult wakeuperResult) {
                    BDebug.i(TAG, "WakeuperListener", "onResult");
                    callbackWakeupResult(wakeuperResult);
                }

                @Override
                public void onError(SpeechError speechError) {
                    BDebug.i(TAG, "WakeuperListener", "onError", speechError.getErrorCode(), speechError.getErrorDescription());
                    RecognizeErrorVO errorVO = new RecognizeErrorVO();
                    errorVO.error = speechError.getErrorDescription();
                    callBackPluginJs(JsConst.ON_WAKEUP_ERROR, DataHelper.gson.toJson(errorVO));
                }

                @Override
                public void onEvent(int i, int i1, int i2, Bundle bundle) {
                    BDebug.i(TAG, "WakeuperListener", "onEvent");
                }

                @Override
                public void onVolumeChanged(int i) {
//                    BDebug.i(TAG, "WakeuperListener", "onVolumeChanged");
                }
            });
        } else {
            BDebug.w(TAG, "startWakeuper error: 唤醒未初始化");
        }
    }

    private void callbackWakeupResult(WakeuperResult results) {
//        String text = JsonParser.parseIatResult(results.getResultString());
        String resultString = results.getResultString();
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callBackPluginJs(JsConst.ON_WAKEUP_RESULT, resultString);

//        callBackPluginJs(ON_READLOCALSOUCE_RESULT, resultBuffer.toString());
    }

    public void stopWakeuper(String[] params) {

        VoiceWakeuper voiceWakeuper = VoiceWakeuper.getWakeuper();
        if (voiceWakeuper != null) {
            voiceWakeuper.destroy();
        }
    }

    public void startWakeuperOneshot(String[] params) {

    }

    public void setWakeUpBuildGrammar(String[] params) {

    }

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        callBackPluginJs(JsConst.ON_RECOGNIZE_RESULT, resultBuffer.toString());

//        callBackPluginJs(ON_READLOCALSOUCE_RESULT, resultBuffer.toString());
    }


    private void callBackPluginJs(String methodName, String jsonData) {
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        evaluateScript(mCallbackWinName, 0, js);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_RECORD_AUDIO_PERMISSION){
            if (grantResults[0] != PackageManager.PERMISSION_DENIED){
                initMsg(mInitParams);
            } else {
                callbackPermissionDenied(permissions);
            }
        }
    }

}
