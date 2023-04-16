package com.faceunity.app_ptag.ui.interaction.weight.history.entity;

import android.os.SystemClock;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 聊天消息
 *
 * @author Richie on 2019.03.27
 */
public class ChatMessage {
    public static final int FROM_USER_TO_GPT = -1;
    public static final int FROM_NLP = 0;
    public static final int FROM_USER = 1;
    public static final int CHAT_TIME = 2;
    public static final int CHAT_EMPTY = 3;
    private static final int DURATION = 60 * 1000;

    private int from;
    private String content;
    private long timeInMills;

    public ChatMessage(int from, String content) {
        this.from = from;
        this.content = content;
        this.timeInMills = System.currentTimeMillis();
        SystemClock.elapsedRealtime();
    }

    public static ChatMessage getNormalMessage(int from, String content) {
        return new ChatMessage(from, content);
    }

    public static ChatMessage getTimeMessage() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        return new ChatMessage(ChatMessage.CHAT_TIME, dateFormat.format(new Date()));
    }

    public static ChatMessage getEmptyMessage() {
        return new ChatMessage(ChatMessage.CHAT_EMPTY, "暂无记录");
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimeInMills() {
        return timeInMills;
    }

    public void setTimeInMills(long timeInMills) {
        this.timeInMills = timeInMills;
    }

    public String formattedTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        return dateFormat.format(new Date(this.timeInMills));
    }

    public boolean is1MinLaterThan(ChatMessage nlpChatMessage) {
        return this.timeInMills - nlpChatMessage.timeInMills > DURATION;
    }

    @Override
    public String toString() {
        return "NlpChatMessage{" +
                "from=" + from +
                ", content='" + content + '\'' +
                ", timeInMills=" + timeInMills +
                '}';
    }
}
