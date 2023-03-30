package com.agora.gpt;

import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;

public class ConvertClient {

    private boolean isInited = false;
    private WeakReference<FragmentActivity> activityRef;
    private static final ConvertClient ourInstance = new ConvertClient();
    private ConvertListener listener;
    private int index=0;

    public static ConvertClient getInstance() {
        return ourInstance;
    }

    private HyUtil.IListener v2tListener = new HyUtil.IListener() {
        // 语音转写文本回调
        @Override
        public void onIstText(String text) {
            if (listener != null) {
                listener.onVoice2Text(index,text);
            }
            ChatGPTManager.getInstance().sendQuestionToChatGPT(index++,text);
        }
    };


    private ConvertClient() {
    }

    public void init(FragmentActivity activity) {
        if (isInited) {
            return;
        }
        isInited = true;
        activityRef = new WeakReference<>(activity);
        //语音转文字初始化
        Voice2TextManager.getInstance().init(activity);
        Voice2TextManager.getInstance().registerListener(v2tListener);
        //联网请求chatgpt初始化
        ChatGPTManager.getInstance().init();
        //文字转语音初始化
        Text2VoiceManager.getInstance().init(activity);
    }

    /**
     * 开始音频转换成文字
     */
    public void startVoice2Text() {
        Voice2TextManager.getInstance().startListening();
    }

    /**
     * 停止语音转文字
     */
    public void stopVoice2Text(){
        Voice2TextManager.getInstance().stopListening();
    }

    /**
     * 设置对chatGPT请求的监听
     *
     * @param convertListener
     */
    public void setChatGPTListener(ConvertListener convertListener) {
        this.listener = convertListener;
        ChatGPTManager.getInstance().setChatGPTListener(convertListener);
        Text2VoiceManager.getInstance().setListener(convertListener);
    }

    /**
     * 开始语音转文字
     * @param text
     */
    public void startText2Voice(int index,String text) {
        Text2VoiceManager.getInstance().startText2Voice(index,text);
    }
}
