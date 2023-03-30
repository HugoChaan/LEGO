package com.agora.gpt;

import android.Manifest;
import android.app.Activity;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.gson.Gson;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.reactivex.disposables.Disposable;
import okhttp3.HttpUrl;

/**
 * Created by 许成谱 on 3/22/23 11:36 上午.
 * qq:1550540124
 * 热爱生活每一天！
 */
class Text2VoiceManager implements LifecycleObserver {
    // 地址与鉴权信息
    public static final String hostUrl = "https://tts-api.xfyun.cn/v2/tts";
    // 均到控制台-语音合成页面获取
    public static final String appid = BuildConfig.plugin_appid;
    public static final String apiSecret =BuildConfig.plugin_api_secret;
    public static final String apiKey = BuildConfig.plugin_api_key;
    // 合成文本
    public static String TEXT = "欢迎来到讯飞开放平台";
    // 合成文本编码格式
    public static final String TTE = "UTF8"; // 小语种必须使用UNICODE编码作为值
    // 发音人参数。到控制台-我的应用-语音合成-添加试用或购买发音人，添加后即显示该发音人参数值，若试用未添加的发音人会报错11200
    public static final String VCN = "xiaoyan";
    // 合成文件名称
//    public static final String OUTPUT_FILE_PATH = "src/main/resources/tts/" + System.currentTimeMillis() + ".pcm";
    // json
    public static final Gson gson = new Gson();
    public static boolean wsCloseFlag = false;

    private RxPermissions rxPermissions;
    private WeakReference<Activity> activityRef;

    private static final Text2VoiceManager ourInstance = new Text2VoiceManager();
    private Disposable disposable;
    private ConvertListener listener;

    static Text2VoiceManager getInstance() {
        return ourInstance;
    }

    private Text2VoiceManager() {
    }

    public void init(FragmentActivity activity) {
        rxPermissions = new RxPermissions(activity);
        activity.getLifecycle().addObserver(this);
        activityRef = new WeakReference<>(activity);
        disposable = rxPermissions
                .request(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                })
                .subscribe(granted -> {
                    if (granted) {
                    }
                });
    }

    // Websocket方法
    public void websocketWork(int index,String wsUrl, String outputFilePath) {
        try {
            OutputStream outputStream = new FileOutputStream(outputFilePath);
            URI uri = new URI(wsUrl);
            WebSocketClient webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    System.out.println("ws建立连接成功...");
                }

                @Override
                public void onMessage(String text) {
                    // System.out.println(text);
                    JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
                    if (myJsonParse.code != 0) {
                        System.out.println("发生错误，错误码为：" + myJsonParse.code);
                        System.out.println("本次请求的sid为：" + myJsonParse.sid);
                    }
                    if (myJsonParse.data != null) {
                        try {
                            byte[] textBase64Decode = Base64Utils.decode(myJsonParse.data.audio).getBytes(StandardCharsets.UTF_8);
                            outputStream.write(textBase64Decode);
                            outputStream.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (myJsonParse.data.status == 2) {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("本次请求的sid==>" + myJsonParse.sid);
                            System.out.println("合成成功，文件保存路径为==>" + outputFilePath);
                            if (listener != null) {
                                listener.onText2Voice(index,outputFilePath);
                            }
                            // 可以关闭连接，释放资源
                            wsCloseFlag = true;
                        }
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    System.out.println("ws链接已关闭，本次请求完成...s="+s);
                }

                @Override
                public void onError(Exception e) {
                    System.out.println("发生错误 " + e.getMessage());
                }
            };
            // 建立连接
            webSocketClient.connect();
            while (!webSocketClient.getReadyState().equals(WebSocket.READYSTATE.OPEN)) {
                //System.out.println("正在连接...");
                Thread.sleep(100);
            }
            MyThread webSocketThread = new MyThread(webSocketClient);
            webSocketThread.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void startText2Voice(int index,String text) {
        Activity activity = activityRef.get();
        if (activity != null) {
            String outputFilePath = activity.getExternalCacheDir().getAbsolutePath()+ File.separator + System.currentTimeMillis() + ".pcm";
            TEXT = text;

            new Thread(){
                @Override
                public void run() {
                    String wsUrl = null;
                    try {
                        wsUrl = getAuthUrl(hostUrl, apiKey, apiSecret).replace("https://", "wss://");

                        websocketWork(index,wsUrl, outputFilePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();

        }
    }

    // 线程来发送音频与参数
    static class MyThread extends Thread {
        WebSocketClient webSocketClient;

        public MyThread(WebSocketClient webSocketClient) {
            this.webSocketClient = webSocketClient;
        }

        public void run() {
            String requestJson;//请求参数json串
            try {
                requestJson = "{\n" +
                        "  \"common\": {\n" +
                        "    \"app_id\": \"" + appid + "\"\n" +
                        "  },\n" +
                        "  \"business\": {\n" +
                        "    \"aue\": \"raw\",\n" +
                        "    \"tte\": \"" + TTE + "\",\n" +
                        "    \"ent\": \"intp65\",\n" +
                        "    \"vcn\": \"" + VCN + "\",\n" +
                        "    \"pitch\": 50,\n" +
                        "    \"speed\": 50\n" +
                        "  },\n" +
                        "  \"data\": {\n" +
                        "    \"status\": 2,\n" +
                        "    \"text\": \"" + Base64.getEncoder().encodeToString(TEXT.getBytes(StandardCharsets.UTF_8)) + "\"\n" +
                        //"    \"text\": \"" + Base64.getEncoder().encodeToString(TEXT.getBytes("UTF-16LE")) + "\"\n" +
                        "  }\n" +
                        "}";
                webSocketClient.send(requestJson);
                // 等待服务端返回完毕后关闭
                while (!wsCloseFlag) {
                    Thread.sleep(200);
                }
                webSocketClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // 鉴权方法
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        //System.out.println(preStr);
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();

        return httpUrl.toString();
    }


    //返回的json结果拆解
    class JsonParse {
        int code;
        String sid;
        Data data;
    }

    class Data {
        int status;
        String audio;
    }

    void setListener(ConvertListener listener) {
        this.listener = listener;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void release() {
        if (disposable != null) {
            disposable.dispose();
        }
    }
}
