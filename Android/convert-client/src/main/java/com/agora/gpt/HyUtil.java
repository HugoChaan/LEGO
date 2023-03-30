/*
 * Copyright (c) Anhui iFLYTEK Universal Language Technology Co., Ltd. 2022-2022. All rights
 * reserved.
 */

package com.agora.gpt;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import io.agora.hy.extension.ExtensionManager;
import io.agora.rtc2.RtcEngine;

/**
 * 寰语工具
 *
 * @author dwyue
 * @since 2022-01-23 10:28:51
 */
public class HyUtil {
    // 请在开放平台申请。测试使用，正式使用需要自己申请。

    /** 应用标识 */
    public static final String APP_ID = BuildConfig.plugin_appid;

    /** API密钥 */
    public static final String API_KEY = BuildConfig.plugin_api_key;

    /** API秘密 */
    public static final String API_SECRET = BuildConfig.plugin_api_secret;

    /** 标签 */
    private static final String TAG = "HyUtil";

    /** {@link Parser} */
    private final Parser mParser = new Parser();

    /** {@link IListener}。非null。 */
    private final IListener mListener;

    /** {@link RtcEngine}。非null。 */
    private final RtcEngine mRtcEngine;

    /**
     * 构造
     *
     * @param rtcEngine {@link RtcEngine}
     */
    public HyUtil(IListener listener, RtcEngine rtcEngine) {
        mListener = listener;
        mRtcEngine = rtcEngine;
    }

    /**
     * 获取参数包装集
     *
     * @return 非空
     */
    public ParamWrap[] getParamWraps() {
        return new ParamWrap[] {
                new ParamWrap("中译英", "zh_cn", "mandarin", "ist_ed_open", 1, "", "cn", "en"),
                new ParamWrap("英译中", "zh_cn", "mandarin", "ist_ed_open", 3, "", "en", "cn"),
        };
    }

    /**
     * 启动倾听
     *
     * @param {@link ParamWrap}。非null。
     */
    public void start(ParamWrap paramWrap) {
        mParser.clear();

        String val = null;
        try {
            JSONObject rootJo = new JSONObject();
            // 公共对象。必选。
            JSONObject commonJo = new JSONObject();
            {
                // 应用标识。必选。
                commonJo.put("app_id", APP_ID);
                // API密钥。必选。
                commonJo.put("api_key", API_KEY);
                // API秘密。必选。
                commonJo.put("api_secret", API_SECRET);
            }
            rootJo.put("common", commonJo);
            // 语音转写对象。必选。
            JSONObject istJo = new JSONObject();
            {
                // URI。必选。
                istJo.put("uri", "wss://ist-api.xfyun.cn/v2/ist");
                // 请求对象。必选。
                JSONObject reqJo = new JSONObject();
                {
                    // 业务对象。必选。
                    JSONObject businessJo = new JSONObject();
                    {
                        // 语种。必选。
                        businessJo.put("language", paramWrap.mIstLanguage);
                        // 口音。必选。
                        businessJo.put("accent", paramWrap.mIstAccent);
                        // 领域。必选。
                        businessJo.put("domain", paramWrap.mIstDomain);
                        // 语言类型
                        // 值类型：int
                        // 值范围：
                        // 1：中英文模式，中文英文均可识别；
                        // 3：英文模式，只识别出英文
                        // 值默认：1
                        businessJo.put("language_type", paramWrap.mIstLanguageType);
                        // 动态修正
                        businessJo.put("dwa", paramWrap.mIstDwa);
                    }
                    reqJo.put("business", businessJo);
                }
                istJo.put("req", reqJo);
            }
            rootJo.put("ist", istJo);
            // 文本翻译对象。
            JSONObject itsJo = new JSONObject();
            {
                // URI。必选。
                itsJo.put("uri", "https://itrans.xfyun.cn/v2/its");
                // 请求对象。必选。
                JSONObject reqJo = new JSONObject();
                {
                    // 业务对象。必选。
                    JSONObject businessJo = new JSONObject();
                    {
                        // 源语种。必选。
                        businessJo.put("from", paramWrap.mItsFrom);
                        // 目标语种。必选。
                        businessJo.put("to", paramWrap.mItsTo);
                    }
                    reqJo.put("business", businessJo);
                }
                itsJo.put("req", reqJo);
            }
            rootJo.put("its", itsJo);
            val = rootJo.toString();
        } catch (JSONException e) {
            mListener.onLogE(TAG + ".start | json fail", e);
            return;
        }
        int errCode = mRtcEngine.setExtensionProperty(ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, "start_listening", val);
        mListener.onLogI(TAG + ".start | mRtcEngine.setExtensionProperty errCode: " + errCode);
    }

    /**
     * 结束音频获取结果
     */
    public void flush() {
        // 值不能为空，否则收不到。
        int errCode = mRtcEngine.setExtensionProperty(ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, "flush_listening", "{}");
        mListener.onLogI(TAG + ".flush | mRtcEngine.setExtensionProperty errCode: " + errCode);
    }

    /**
     * 停止倾听
     */
    public void stop() {
        int errCode = mRtcEngine.setExtensionProperty(ExtensionManager.EXTENSION_VENDOR_NAME,
                ExtensionManager.EXTENSION_AUDIO_FILTER_NAME, "stop_listening", "{}");
        mListener.onLogI(TAG + ".stop | mRtcEngine.setExtensionProperty errCode: " + errCode);
    }

    /**
     * 事件回调
     *
     * @param key 键
     * @param val 值
     */
    public void onEvent(String key, String val) {
        mParser.onEvent(key, val);
    }

    /** 监听器 */
    public interface IListener {
        /**
         * logcatI回调
         *
         * @param tip 非null
         */
       default void onLogI(String tip){}

        /**
         * logcatE回调
         *
         * @param tip 非null
         */
        default void onLogE(String tip){}

        /**
         * logcatE回调
         *
         * @param tip 非null
         * @param tr 非null
         */
        default void onLogE(String tip, Throwable tr){}

        /**
         * 语音转写文本回调
         *
         * @param text 非null
         */
        default void onIstText(String text){}


        /**
         * 文本翻译文本回调
         *
         * @param text 非null
         */
        default void onItsText(String text){}
    }

    /** 参数包装 */
    public class ParamWrap {
        /** 名称。非空。 */
        public final String mName;

        /** {@link HyUtil#start(ParamWrap)} */
        public final String mIstLanguage;

        /** {@link HyUtil#start(ParamWrap)} */
        public final String mIstAccent;

        /** {@link HyUtil#start(ParamWrap)} */
        public final String mIstDomain;

        /** {@link HyUtil#start(ParamWrap)} */
        public final int mIstLanguageType;

        /** {@link HyUtil#start(ParamWrap)} */
        public final String mIstDwa;

        /** {@link HyUtil#start(ParamWrap)} */
        public final String mItsFrom;

        /** {@link HyUtil#start(ParamWrap)} */
        public final String mItsTo;

        /**
         * 构造
         *
         * @param name {@link #mName}
         * @param istLanguage {@link #mIstLanguage}
         * @param istAccent {@link #mIstAccent}
         * @param istDomain {@link #mIstDomain}
         * @param istLanguageType {@link #mIstLanguageType}
         * @param istDwa {@link #mIstDwa}
         * @param itsFrom {@link #mItsFrom}
         * @param itsTo {@link #mItsTo}
         */
        public ParamWrap(String name, String istLanguage, String istAccent, String istDomain,
                int istLanguageType, String istDwa, String itsFrom, String itsTo) {
            mName = name;
            mIstLanguage = istLanguage;
            mIstAccent = istAccent;
            mIstDomain = istDomain;
            mIstLanguageType = istLanguageType;
            mIstDwa = istDwa;
            mItsFrom = itsFrom;
            mItsTo = itsTo;
        }
    }

    /** 解析器 */
    private class Parser {
        /** 标签 */
        private static final String TAG = HyUtil.TAG + "." + "Parser";

        /** 文本集集。条目[0]：语音转写文本；[1]：文本翻译文本。 */
        private final List<String[]> mTextss = new ArrayList<>(10);

        /** 索引集 */
        private final LinkedBlockingQueue<Integer> mIdxs = new LinkedBlockingQueue<>(3);

        /**
         * 构造
         */
        public Parser() {
            clear();
        }

        /**
         * 事件回调
         *
         * @param key 键
         * @param val 值
         */
        public void onEvent(String key, String val) {
            switch (key) {
            case "error": {
                onError(val);
                break;
            }
            case "ist_result": {
                onIstResult(val);
                break;
            }
            case "its_result": {
                onItsResult(val);
                break;
            }
            case "end": {
                onEnd(val);
                break;
            }
            default: {
                Log.e(TAG, "onEvent | inv key: " + key);
                break;
            }
            }
        }

        /**
         * 错误回调
         *
         * @param val 值
         */
        private void onError(String val) {
            mListener.onLogE(TAG + ".onError | " + val);
        }

        /**
         * 语音转写结果回调
         *
         * @param val 值
         */
        private void onIstResult(String val) {
            boolean isSubEnd=false;
            try {
                isSubEnd= parseIstResult(val);
            } catch (Exception e) {
                mListener.onLogE(TAG + ".onIstResult | parseIstResult fail, val: " + val, e);
                stop();
                return;
            }
            if(isSubEnd) {
                final String text = getDisplayText();
                Log.i(TAG, "onIstResult | text: " + text);
                mListener.onIstText(text);
            }
        }

        /**
         * 文本翻译结果回调
         *
         * @param val 值
         */
        private void onItsResult(String val) {
            try {
                parseItsResult(val);
            } catch (Exception e) {
                mListener.onLogE(TAG + ".onItsResult | parseItsResult fail, val: " + val, e);
                stop();
                return;
            }
            final String text = getItsDisplayText();
            mListener.onItsText(text);
        }

        /**
         * 结束回调
         *
         * @param val 值
         */
        private void onEnd(String val) {
            mListener.onLogI(TAG + ".onEnd");
        }

        /**
         * 解析语音转写结果
         *
         * @param result 结果。非空。
         * @return 是否结束
         * @throws Exception 失败
         */
        private boolean parseIstResult(String result) throws Exception {
            JSONObject rootJo = new JSONObject(result);
            int code = rootJo.getInt("code");
            if (0 != code) {
                throw new Exception("Parser.parseIstResult | code: " + code);
            }
            // String message = rootJo.getString("message");
            // String sid = rootJo.getString("sid");
            JSONObject dataJo = rootJo.optJSONObject("data");
            if (null == dataJo) {
                Log.w(TAG, "parseIstResult | dataJo: null, result: " + result);
                return false;
            }

            int status = dataJo.getInt("status");
            boolean isEnd = 2 == status;
            JSONObject resultJo = dataJo.optJSONObject("result");
            if (null == resultJo) {
                Log.w(TAG, "parseIstResult | resultJo: null, result: " + result);
                return isEnd;
            }

            // 禁止中途return
            StringBuilder sb = new StringBuilder();

            int sn = resultJo.getInt("sn");
            boolean subEnd = resultJo.optBoolean("sub_end", true);
            String pgs = resultJo.optString("pgs", null);
            JSONArray rgJa = null;
            if ("rpl".equals(pgs)) {
                rgJa = resultJo.optJSONArray("rg");
            }
            JSONArray wsJa = resultJo.optJSONArray("ws");
            if (null == wsJa) {
                Log.w(TAG, "parseIstResult | wsJa: null, result: " + result);
            } else {
                for (int i = 0; wsJa.length() > i; ++i) {
                    JSONObject wsJaItemJo = wsJa.optJSONObject(i);
                    if (null == wsJaItemJo) {
                        Log.w(TAG, "parseIstResult | wsJaItemJo: null i: " + i + ", result: "
                                + result);
                        continue;
                    }

                    JSONArray cwJa = wsJaItemJo.optJSONArray("cw");
                    if (null == cwJa) {
                        Log.w(TAG, "parseIstResult | cwJa: null i: " + i + ", result: "
                                + result);
                        continue;
                    }

                    JSONObject cwJaItem0Jo = cwJa.optJSONObject(0);
                    if (null == cwJaItem0Jo) {
                        Log.w(TAG, "parseIstResult | cwJaItem0Jo: null i: " + i
                                + ", result: " + result);
                        continue;
                    }

                    String w = cwJaItem0Jo.optString("w", null);
                    String wp = cwJaItem0Jo.optString("wp", null);
                    if (TextUtils.isEmpty(w)) {
                        continue;
                    }
                    if(sb.length()==0&&TextUtils.equals(wp,"p")) {
                        continue;
                    }
                    sb.append(w);
                }
            }

            // 替换
            if (null != rgJa) {
                for (int i = rgJa.getInt(0) - 1; rgJa.getInt(1) - 1 >= i; ++i) {
                    mTextss.set(i, null);
                }
            }
            // 追加
            String text = sb.toString();
            mTextss.add(new String[] { text, null });
            // 句空结果没必要加入导致清空，优化显示效果。
            if ((subEnd || isEnd) && !TextUtils.isEmpty(text)) {
                mIdxs.offer(sn);
            }

            return subEnd;
        }
        //
        // /**
        //  * 获取全部文本
        //  *
        //  * @return 非null
        //  */
        // public String getAllText() {
        //     StringBuilder sb = new StringBuilder();
        //     for (String text : mTexts) {
        //         if (null != text) {
        //             sb.append(text);
        //         }
        //     }
        //     return sb.toString();
        // }

        /**
         * 获取显示文本
         *
         * @return 非null
         */
        private String getDisplayText() {
            // 过早的不显示了。最多显示3句，更早的清空。
            if (0 == mIdxs.remainingCapacity()) {
                int begin = mIdxs.poll();
                int end = mIdxs.peek();
                // 释放内存
                for (int i = begin; end > i; ++i) {
                    mTextss.set(i, null);
                }
            }

            StringBuilder sb = new StringBuilder();
            int size = mTextss.size();
            for (int i = mIdxs.peek(); size > i; ++i) {
                String[] texts = mTextss.get(i);
                if (null == texts) {
                    continue;
                }
                sb.append(texts[0]);
            }
            return sb.toString();
        }

        /**
         * 解析文本翻译结果
         *
         * @param result 结果。非空。
         * @throws Exception 失败
         */
        private void parseItsResult(String result) throws Exception {
            JSONObject rootJo = new JSONObject(result);
            int code = rootJo.getInt("code");
            if (0 != code) {
                throw new Exception("Parser.parseItsResult | code: " + code);
            }
            int ist_sn = rootJo.getInt("ist_sn");
            JSONObject dataJo = rootJo.getJSONObject("data");
            JSONObject resultJo = dataJo.getJSONObject("result");
            JSONObject transResultJo = resultJo.getJSONObject("trans_result");
            final String dst = transResultJo.getString("dst");

            mTextss.get(ist_sn - 1)[1] = dst;
        }

        /**
         * 获取文本翻译显示文本
         *
         * @return 非null
         */
        private String getItsDisplayText() {
            StringBuilder sb = new StringBuilder();
            int size = mTextss.size();
            for (int i = mIdxs.peek(); size > i; ++i) {
                String[] texts = mTextss.get(i);
                if (null == texts) {
                    continue;
                }
                String text = texts[1];
                if (TextUtils.isEmpty(text)) {
                    continue;
                }
                sb.append(text);
            }
            return sb.toString();
        }

        /**
         * 清空
         */
        public void clear() {
            mTextss.clear();
            mIdxs.clear();
            mIdxs.offer(0);
        }
    }
}
