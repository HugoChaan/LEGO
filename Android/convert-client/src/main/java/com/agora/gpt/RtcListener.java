package com.agora.gpt;

public interface RtcListener {
    void onJoinChannelSuccess(String channel, int uid, int elapsed);

    void onStreamMessage(int uid, int streamId, byte[] data);

    void onUserJoined(int uid, int elapsed);
}
