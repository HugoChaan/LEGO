package com.faceunity.app_ptag.ui.interaction.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录音工具
 *
 * @author Richie on 2018.10.15
 */
public final class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    // 音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 44.1k 所有设备都支持，这里用 16k
    private final static int AUDIO_SAMPLE_RATE = 16000;
    // 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    // 编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 线程池
    private final ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    // 缓冲区字节大小
    private int mBufferSizeInBytes;
    // 录音对象
    private volatile AudioRecord mAudioRecord;
    // 录音状态
    private volatile Status mStatus = Status.STATUS_NO_READY;
    // 录音监听
    private RecordStreamListener mRecordStreamListener;

    /**
     * 创建默认的录音对象
     */
    public void createDefaultAudio() {
        createAudio(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
    }

    /**
     * 创建录音对象
     *
     * @param audioSource
     * @param sampleRateInHz
     * @param channelConfig
     * @param audioFormat
     */
    public void createAudio(int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
        // 获得缓冲区字节大小
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, mBufferSizeInBytes);
        int state = mAudioRecord.getState();
        Log.i(TAG, "createAudio state:" + state + ", initialized:" + (state == AudioRecord.STATE_INITIALIZED));
        mStatus = Status.STATUS_READY;
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        if (mStatus == Status.STATUS_NO_READY || mAudioRecord == null) {
            throw new IllegalStateException("录音尚未初始化");
        }
        if (mStatus == Status.STATUS_START) {
            throw new IllegalStateException("正在录音...");
        }
        Log.d(TAG, "===startRecord===");
        mAudioRecord.startRecording();

        //将录音状态设置成正在录音状态
        mStatus = Status.STATUS_START;

        //使用线程池管理线程
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    writeDataToStream();
                } catch (Exception e) {
                    Log.e(TAG, "writeDataToStream: ", e);
                    if (mRecordStreamListener != null) {
                        mRecordStreamListener.onError(e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        Log.d(TAG, "===stopRecord===");
        if (mStatus == Status.STATUS_NO_READY || mStatus == Status.STATUS_READY) {
            throw new IllegalStateException("录音尚未开始");
        } else {
            mStatus = Status.STATUS_STOP;
            mAudioRecord.stop();
            release();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "===release===");
        mStatus = Status.STATUS_NO_READY;

        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
    }

    /**
     * 取消录音
     */
    public void canel() {
        Log.d(TAG, "===cancel===");
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        mStatus = Status.STATUS_NO_READY;
    }

    /**
     * 将音频信息写入字节流
     */
    private void writeDataToStream() throws Exception {
        /**
         * 内存缓冲
         */
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byte[] audioData = new byte[mBufferSizeInBytes];
            long startTime = System.currentTimeMillis();
            if (mRecordStreamListener != null) {
                mRecordStreamListener.onStart();
            }
            long duration = 0;
            while (mStatus == Status.STATUS_START) {
                int readSize = mAudioRecord.read(audioData, 0, mBufferSizeInBytes);
                if (AudioRecord.ERROR_INVALID_OPERATION != readSize && AudioRecord.ERROR_BAD_VALUE != readSize
                        && AudioRecord.ERROR_DEAD_OBJECT != readSize && AudioRecord.ERROR != readSize) {
                    byteArrayOutputStream.write(audioData, 0, readSize);
                    duration = System.currentTimeMillis() - startTime;
                    if (mRecordStreamListener != null) {
                        mRecordStreamListener.onRecording(duration);
                    }
                }
            }
            byteArrayOutputStream.flush();
            if (mRecordStreamListener != null) {
                mRecordStreamListener.onFinish(duration, byteArrayOutputStream.toByteArray());
            }
        } finally {
            byteArrayOutputStream.close();
        }
    }

    public void setRecordStreamListener(RecordStreamListener recordStreamListener) {
        this.mRecordStreamListener = recordStreamListener;
    }

    /**
     * 录音对象的状态
     */
    public enum Status {
        //未开始
        STATUS_NO_READY,
        //预备
        STATUS_READY,
        //录音
        STATUS_START,
        //停止
        STATUS_STOP
    }

    /**
     * invoked from work thread
     */
    public interface RecordStreamListener {
        /**
         * 开始录音
         */
        void onStart();

        /**
         * 录音过程中
         *
         * @param duration
         */
        void onRecording(long duration);

        /**
         * 录音完成
         *
         * @param duration
         * @param pcmData
         */
        void onFinish(long duration, byte[] pcmData);

        /**
         * 发生错误
         *
         * @param error
         */
        void onError(String error);
    }
}

