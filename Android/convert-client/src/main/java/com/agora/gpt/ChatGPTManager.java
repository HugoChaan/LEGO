package com.agora.gpt;

import android.util.Log;

import com.blankj.utilcode.util.ThreadUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


class ChatGPTManager {
    private final String Tag = getClass().getSimpleName();
    private static final ChatGPTManager ourInstance = new ChatGPTManager();
    private ConvertListener listener;
    private boolean isInited = false;
    private static final String API_KEY = BuildConfig.chatgpt_api_key;
    private static final String API_BASE_URL = BuildConfig.chatgpt_api_base_url;
    private OkHttpClient client;
    MediaType mediaType = MediaType.parse("application/json");
    private RequestBody requestBody = new RequestBody();

    static ChatGPTManager getInstance() {
        return ourInstance;
    }

    private ChatGPTManager() {
    }

    public void init() {
        if (isInited) {
            return;
        }
        isInited = true;
        client = new OkHttpClient()
                .newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();
        requestBody.setMessages(new FixSizeList<>(20));
        requestBody.setModel(ChatGptModel.GPT_35);
    }

    public void setMemoryLength(int length) {
        requestBody.getMessages().setMemoryLength(length);
    }

    public void setSystem(String content) {
        RequestBody.Messages messages = new RequestBody.Messages();
        messages.setRole("system");
        messages.setContent(content);
        requestBody.getMessages().addSystemParams(messages);
        requestBody.getMessages().removeAllHistory();
    }


    public RequestBody getRequestBody() {
        return requestBody;
    }

    public void setChatGPTListener(ConvertListener listener) {
        this.listener = listener;
    }

    public void sendQuestionToChatGPT(int index, String question) {
        if (!isInited) {
            return;
        }
        ThreadUtils.getCachedPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = executeRequest(question);
                    if (listener != null) {
                        listener.onQues2AnsSuccess(index, question, response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onFailure(0, e.getMessage());
                    }
                }
            }
        });

    }

    private String executeRequest(String question) throws IOException {
        RequestBody.Messages messages = new RequestBody.Messages();
        messages.setRole("user");
        messages.setContent(question);
        requestBody.getMessages().add(messages);
        String requestBodyStr = new Gson().toJson(requestBody);
        okhttp3.RequestBody body = okhttp3.RequestBody.create(
                mediaType,
                requestBodyStr
        );

        if (ConvertClient.getInstance().isDebugMode()) {
            Log.d(Tag, "convert requestBody=" + requestBodyStr);
        }

        Request request = new Request.Builder()
                .url(API_BASE_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("agora-service-key", API_KEY)
                .build();

        Response response = client.newCall(request)
                .execute();

        if (!response.isSuccessful()) {
            throw new IOException("Unexpected response code: " + response);
        }
        String responseBody = response.body().string();
        ChatGPTBean result = new Gson().fromJson(responseBody, ChatGPTBean.class);
        RequestBody.Messages messages_response = new RequestBody.Messages();
        messages_response.setRole("assistant");
        messages_response.setContent(result.getAnswer());
        requestBody.getMessages().add(messages_response);

        if (ConvertClient.getInstance().isDebugMode()) {
            Log.d(Tag, "convert result=" + result.getAnswer());
        }
        return result.getAnswer();
    }

    public void setChatGptModel(ChatGptModel model) {
        requestBody.setModel(model);
    }
}
