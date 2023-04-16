package com.agora.gpt;

import android.view.TextureView;

import androidx.fragment.app.FragmentActivity;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

public class ConvertClient {

    private boolean isInited = false;
    private WeakReference<FragmentActivity> activityRef;
    private static final ConvertClient ourInstance = new ConvertClient();
    private ConvertListener listener;
    private int index = 0;
    private boolean debugMode;
    private boolean isAutoVoice2Text = false;

    public static ConvertClient getInstance() {
        return ourInstance;
    }

    private HyUtil.IListener v2tListener = new HyUtil.IListener() {
        // 语音转写文本回调
        @Override
        public void onIstText(String text) {
            if (listener != null) {
                listener.onVoice2Text(index, text);
            }
            if (isAutoVoice2Text) {
                ChatGPTManager.getInstance().sendQuestionToChatGPT(index, text);
            }
            index++;
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
        Voice2TextManager.getInstance().setRtcListener(new RtcListener() {
            @Override
            public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                if (listener != null) {
                    listener.onJoinChannelSuccess(channel,uid, elapsed);
                }
            }

            @Override
            public void onStreamMessage(int uid, int streamId, byte[] data) {
                if (listener != null) {
                    listener.onStreamMessage(uid,streamId, data);
                }
            }

            @Override
            public void onUserJoined(int uid, int elapsed) {
                if (listener != null) {
                    listener.onUserJoined(uid, elapsed);
                }
            }
        });
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
    public void stopVoice2Text() {
        Voice2TextManager.getInstance().stopListening();
    }

    public void flushVoice2Text() {
        Voice2TextManager.getInstance().flushListening();
    }

    /**
     * 问chatgpt问题
     *
     * @param markIndex 用于标记question的索引，可随便传一个
     * @param question  要问chat-gpt的问题
     */
    public void sendQuestionToChatGPT(int markIndex, String question) {
        ChatGPTManager.getInstance().sendQuestionToChatGPT(markIndex, question);
    }

    /**
     * 问chatgpt问题
     *
     * @param question 要问chat-gpt的问题
     */
    public void sendQuestionToChatGPT(String question) {
        ChatGPTManager.getInstance().sendQuestionToChatGPT(-1, question);
    }

    /**
     * 拿到请求chat-gpt的请求体
     *
     * @return
     */
    public RequestBody getRequestBody() {
        return ChatGPTManager.getInstance().getRequestBody();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * 设置是否debug模式，true:开启日志 false:关闭内部日志
     *
     * @param debugMode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * 设置请求chatgpt的系统system预设，注意会清除掉以前的记忆
     *
     * @param content
     */
    public void setSystem(String content) {
        ChatGPTManager.getInstance().setSystem(content);
    }

    /**
     * 设置请求chat-gpt记忆条数，不设置的话默认20条记忆
     *
     * @param length
     */
    public void setMemoryLength(int length) {
        ChatGPTManager.getInstance().setMemoryLength(length);
    }

    /**
     * 设置chan-gpt 的模型
     * @param model  参考{@link ChatGptModel}
     */
    public void setChatGptModel(ChatGptModel model){
        ChatGPTManager.getInstance().setChatGptModel(model);
    }

    /**
     * 是否自动语音转文字
     *
     * @return
     */
    public boolean isAutoVoice2Text() {
        return isAutoVoice2Text;
    }

    /**
     * 设置是否自动语音转文字
     *
     * @param autoVoice2Text
     */
    public void setAutoVoice2Text(boolean autoVoice2Text) {
        isAutoVoice2Text = autoVoice2Text;
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
     *
     * @param text
     */
    public void startText2Voice(int index, String text) {
        Text2VoiceManager.getInstance().startText2Voice(index, text);
    }



    // Agora SDK

    public void joinChannel(String channelName) {
        Voice2TextManager.getInstance().joinChannel(channelName);
    }

    public void leaveChannel() {
        Voice2TextManager.getInstance().leaveChannel();
    }

    public void enableLocalView(TextureView view) {
        Voice2TextManager.getInstance().enableLocalView(view);
    }

    public void enableRemoteView(TextureView view, int remoteUid) {
        Voice2TextManager.getInstance().enableRemoteView(view, remoteUid);
    }

    public void sendStreamMessage(JSONObject json) {
        Voice2TextManager.getInstance().sendStreamMessage(json);
    }

    public void enableSTT(boolean enable) {
        Voice2TextManager.getInstance().enableSTT(enable);
    }
}
