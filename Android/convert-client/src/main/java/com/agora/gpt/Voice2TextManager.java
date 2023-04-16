package com.agora.gpt;

import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_FIT;
import static io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.gson.JsonObject;
import com.tbruyelle.rxpermissions2.RxPermissions;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Random;

import io.agora.hy.extension.ExtensionManager;
import io.agora.rtc2.Constants;
import io.agora.rtc2.DataStreamConfig;
import io.agora.rtc2.IMediaExtensionObserver;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.video.VideoEncoderConfiguration;
import io.reactivex.disposables.Disposable;

class Voice2TextManager implements LifecycleObserver {
    private static final String appId = "6bb480f77c6c458696eadb61dfc6fb76";
    private final String TAG = getClass().getSimpleName();
    private final static String channelName = "agora_extension1";
    private RtcEngine mRtcEngine;
    private Context mContext;
    private boolean isInited = false;
    private WeakReference<Activity> activityRef;
    private HyUtil mHyUtil = null;
    private HyUtil.ParamWrap[] mParamWraps = null;
    private RxPermissions rxPermissions;
    private Disposable disposable;
    private static final Voice2TextManager ourInstance = new Voice2TextManager();
    private static final String[] REQUESTED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private int streamId = 0;

    private RtcListener mRtcListener;
    public void setRtcListener(RtcListener listen) {
        this.mRtcListener = listen;
    }

    private final IMediaExtensionObserver mMediaExtensionObserver = new IMediaExtensionObserver() {
        @Override
        public void onEvent(String provider, String extension, String key, String value) {
            Log.i(TAG, "onEvent | provider: " + provider + ", extension: " + extension
                    + ", key: " + key + ", value: " + value);
            if (!ExtensionManager.EXTENSION_VENDOR_NAME.equals(provider)
                    || !ExtensionManager.EXTENSION_AUDIO_FILTER_NAME.equals(extension)) {
                return;
            }
            mHyUtil.onEvent(key, value);
        }

        @Override
        public void onStarted(String provider, String extension) {
            Log.i(TAG, "onStarted | provider: " + provider + ", extension: " + extension);
        }

        @Override
        public void onStopped(String provider, String extension) {
            Log.i(TAG, "onStarted | provider: " + provider + ", extension: " + extension);
        }

        @Override
        public void onError(String provider, String extension, int errCode, String errMsg) {
            Log.e(TAG, "onStarted | provider: " + provider + ", extension: " + extension
                    + ", errCode: " + errCode + ", errMsg: " + errMsg);
        }
    };

    private final HyUtil.IListener mHyUtilListener = new HyUtil.IListener() {
        @Override
        public void onLogI(String tip) {
            Log.i(TAG, tip);
        }

        @Override
        public void onLogE(String tip) {
            Log.e(TAG, tip);
        }

        @Override
        public void onLogE(String tip, Throwable tr) {
            String inTip = tip + ", err: " + tr.toString();
            Log.e(TAG, inTip);
        }

        @Override
        public void onIstText(final String text) {
            Log.i(TAG, "语音转文本结果:"+text);
            if(v2tListener!=null) {
                v2tListener.onIstText(text);
            }
        }

        @Override
        public void onItsText(final String text) {
            Log.i(TAG, "文本翻译结果:"+text);
        }
    };
    private HyUtil.IListener v2tListener;

    static Voice2TextManager getInstance() {
        return ourInstance;
    }

    private Voice2TextManager() {
    }

    void init(FragmentActivity activity) {
        if (isInited) {
            return;
        }
        isInited = true;
        rxPermissions = new RxPermissions(activity);
        activityRef = new WeakReference<>(activity);
        mContext = activity.getApplicationContext();
        // 保持亮屏
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activity.getLifecycle().addObserver(this);
        disposable = rxPermissions
                .request(REQUESTED_PERMISSIONS)
                .subscribe(granted -> {
                    if (granted) {
                        initAgoraEngine();
                    }
                });
    }

    private void initAgoraEngine() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = mContext;
            config.mAppId = appId;
            //Name of dynamic link library is provided by plug-in vendor,
            //e.g. libagora-bytedance.so whose EXTENSION_NAME should be "agora-bytedance"
            //and one or more plug-ins can be added
            config.addExtension(ExtensionManager.EXTENSION_NAME);
            config.mExtensionObserver = mMediaExtensionObserver;
            config.mEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String s, int i, int i1) {
                    Log.d("LEGO", "mRtcEngine onJoinChannelSuccess");
                    mRtcListener.onJoinChannelSuccess(s, i, i1);
                    mRtcEngine.adjustPlaybackSignalVolume(0);
                }

                @Override
                public void onStreamMessage(int uid, int streamId, byte[] data) {
                    Log.d("LEGO", "onStreamMessage");
                    mRtcListener.onStreamMessage(uid, streamId, data);
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                    Log.d("LEGO", "onUserJoined");
                    mRtcListener.onUserJoined(uid, elapsed);
                }
            };
            mRtcEngine = RtcEngine.create(config);
            mRtcEngine.setParameters("{\"che.audio.ains_mode\":2}");
            mRtcEngine.setParameters("{\"che.audio.nsng.lowerBound\":10}");
            mRtcEngine.setParameters("{\"che.audio.nsng.lowerMask\":10}");
            mRtcEngine.setParameters("{\"che.audio.nsng.statisticalbound\":0}");
            mRtcEngine.setParameters("{\"che.audio.nsng.finallowermask\":8}");
            mRtcEngine.setParameters("{\"che.audio.nsng.enhfactorstastical\":200}");
            mRtcEngine.enableExtension(ExtensionManager.EXTENSION_VENDOR_NAME,
                    ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, true);
            // // 设置logcat日志等级
            // // 值类型：int
            // // 值范围：
            // // LOG_LVL_UNKNOWN：0；
            // // LOG_LVL_DEFAULT：1；
            // // LOG_LVL_VERBOSE：2；
            // // LOG_LVL_DEBUG：3；
            // // LOG_LVL_INFO：4；
            // // LOG_LVL_WARN：5；
            // // LOG_LVL_ERROR：6；
            // // LOG_LVL_FATAL：7；
            // // LOG_LVL_SILENT：8。
            // // 值默认：LOG_LVL_UNKNOWN
            // mRtcEngine.setExtensionProviderProperty(ExtensionManager.EXTENSION_VENDOR_NAME,
            //         "log_lvl", "" + 8);
            VideoEncoderConfiguration configuration = new VideoEncoderConfiguration(640, 360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE);
            mRtcEngine.setVideoEncoderConfiguration(configuration);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            // // 试验48k双声道
            // mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO);
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            mRtcEngine.enableLocalVideo(true);
            mRtcEngine.enableVideo();
            mRtcEngine.enableAudio();
            Log.d(TAG, "api call join channel");

            DataStreamConfig dataStreamConfig = new DataStreamConfig();
            dataStreamConfig.ordered = false;
            dataStreamConfig.syncWithAudio = false;
            streamId = mRtcEngine.createDataStream(dataStreamConfig);

            mRtcEngine.startPreview();

            mHyUtil = new HyUtil(mHyUtilListener, mRtcEngine);
            mParamWraps = mHyUtil.getParamWraps();
            // 名称集
            String[] names = new String[mParamWraps.length];
            for (int i = 0; mParamWraps.length > i; ++i) {
                names[i] = mParamWraps[i].mName;
            }
        } catch (Exception e) {
            Log.e(TAG, "initAgoraEngine | fail", e);
        }
    }

    public void joinChannel(String channelName) {
        mRtcEngine.joinChannel(appId, channelName, "", new Random().nextInt(1000000) + 10000);
        mRtcEngine.enableExtension(ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, false);
    }

    public void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    public void enableSTT(boolean enable) {
        mRtcEngine.enableExtension(ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, enable);
    }

    public void enableLocalView(TextureView view) {
        mRtcEngine.setupLocalVideo(new VideoCanvas(view));
    }

    public void enableRemoteView(TextureView view, int remoteUid) {
        mRtcEngine.setupRemoteVideo(new VideoCanvas(view, RENDER_MODE_HIDDEN, remoteUid));
    }

    public void sendStreamMessage(JSONObject jsonMsg) {
        int ret = mRtcEngine.sendStreamMessage(streamId, jsonMsg.toString().getBytes());
        if (ret < 0) {
            Log.e(TAG, "sendStreamMessage | fail: "+ ret);
        }
    }


    void startListening() {

        HyUtil.ParamWrap paramWrap = mParamWraps[0];
        mHyUtil.start(paramWrap);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void release() {
        if (disposable != null) {
            disposable.dispose();
        }
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            RtcEngine.destroy();
        }
    }

    public void registerListener(HyUtil.IListener v2tListener) {
        this.v2tListener=v2tListener;
    }

    public void stopListening() {
        mHyUtil.stop();
    }

    public void flushListening() {
        mHyUtil.flush();
    }
}
