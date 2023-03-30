package com.agora.gpt;

import com.blankj.utilcode.util.ThreadUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


class ChatGPTManager {
    private static final ChatGPTManager ourInstance = new ChatGPTManager();
    private ConvertListener listener;
    private boolean isInited=false;
    private static final String API_KEY = BuildConfig.chatgpt_api_key;
    private static final String API_BASE_URL = BuildConfig.chatgpt_api_base_url;
    private OkHttpClient client;
    MediaType mediaType = MediaType.parse("application/json");

    static ChatGPTManager getInstance() {
        return ourInstance;
    }

    private ChatGPTManager() {
    }

    public void init(){
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
    }

    public void setChatGPTListener(ConvertListener listener){
        this.listener =listener;
    }

    public void sendQuestionToChatGPT(int index,String question)  {
        if(!isInited) {
            return;
        }
        ThreadUtils.getCachedPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String response = executeRequest(question);
                    if(listener!=null) {
                        listener.onQues2AnsSuccess(index,question,response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if(listener!=null) {
                        listener.onFailure(0,e.getMessage());
                    }
                }
            }
        });

    }

    private String executeRequest(String question) throws IOException {
        RequestBody body = RequestBody.create(
                mediaType,
                "{\n" +
                        "    \"questions\":[ \"" + question + "\"]\n" +
//                        "    \"temperature\": 0.5,\n" +
//                        "    \"max_tokens\": 1000,\n" +
//                        "    \"n\": 1,\n" +
//                        "    \"stop\": null,\n" +
//                        "    \"model\": \"text-davinci-003\"\n" +
                        "}");

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
        return result.getAnswer();
    }
}
