package org.zywx.wbpalmstar.plugin.uexxunfei;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;

import org.zywx.wbpalmstar.engine.DataHelper;
import org.zywx.wbpalmstar.engine.EBrowserView;
import org.zywx.wbpalmstar.engine.universalex.EUExBase;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitInputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitOutputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitRecognizerInputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitRecognizerOutputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitSpeakerInputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.InitSpeakerOutputVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.RecognizeErrorVO;
import org.zywx.wbpalmstar.plugin.uexxunfei.vo.StartSpeakingVO;

public class EUExXunfei extends EUExBase {

    private static final String BUNDLE_DATA = "data";

    private SpeechSynthesizer mTts = null;
    private SpeechRecognizer mIat = null;

    private static final int MSG_INIT = 1;
    private static final int MSG_INIT_SPEAKER = 2;

    private String mCallbackWinName="root";

    public EUExXunfei(Context context, EBrowserView eBrowserView) {
        super(context, eBrowserView);
    }

    @Override
    protected boolean clean() {
        return false;
    }


    public void init(String[] params) {
        if (params == null || params.length < 1) {
            errorCallback(0, 0, "error params!");
            return;
        }
        Message msg = new Message();
        msg.obj = this;
        msg.what = MSG_INIT;
        Bundle bd = new Bundle();
        bd.putStringArray(BUNDLE_DATA, params);
        msg.setData(bd);
        mHandler.sendMessage(msg);
    }

    private void initMsg(String[] params) {
        mCallbackWinName=mBrwView.getWindowName();
        String json = params[0];
        InitInputVO initInputVO = DataHelper.gson.fromJson(json, InitInputVO.class);
        SpeechUtility speechUtility = SpeechUtility.createUtility(mContext.getApplicationContext(), SpeechConstant
                .APPID + "=" +
                initInputVO.appID);
        InitOutputVO outputVO = new InitOutputVO();
        outputVO.result = (speechUtility != null);
        callBackPluginJs(JsConst.CALLBACK_INIT, DataHelper.gson.toJson(outputVO));
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
        String json = params[0];
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

    private void callBackPluginJs(String methodName, String jsonData) {
        String js = SCRIPT_HEADER + "if(" + methodName + "){"
                + methodName + "('" + jsonData + "');}";
        evaluateScript(mCallbackWinName,0,js);
    }

}
