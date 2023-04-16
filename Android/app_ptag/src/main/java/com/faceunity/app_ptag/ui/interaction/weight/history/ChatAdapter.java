package com.faceunity.app_ptag.ui.interaction.weight.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.faceunity.app_ptag.R;
import com.faceunity.app_ptag.ui.interaction.weight.history.entity.ChatMessage;

import java.util.List;

/**
 * @author changwei on 2021/11/2 3:10 下午
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.TTsVH> {
    private List<ChatMessage> mChatMessages;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        mChatMessages = chatMessages;
    }

    public void add(ChatMessage chatMessage) {
        // 获取最后一条信息的时间戳，然后和当前的作对比，大于一分钟就显示当前时间
        int itemCount = mChatMessages.size();
        if (itemCount > 0) {
            ChatMessage lastChatMessage = mChatMessages.get(itemCount - 1);
            if (chatMessage.is1MinLaterThan(lastChatMessage)) {
                showChatTime();
            }
        } else {
            showChatTime();
        }
        // 同步数据集
        notifyItemRangeRemoved(0, mChatMessages.size());
        mChatMessages.add(chatMessage);
        notifyItemInserted(mChatMessages.lastIndexOf(chatMessage));
    }

    // 添加时间
    private void showChatTime() {
        ChatMessage chatTime = ChatMessage.getTimeMessage();
        mChatMessages.add(chatTime);
        notifyItemInserted(mChatMessages.lastIndexOf(chatTime));
    }

    @NonNull
    @Override
    public TTsVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == ChatMessage.FROM_NLP) {
            view = layoutInflater.inflate(R.layout.recycler_chat_left, parent, false);
        } else if (viewType == ChatMessage.FROM_USER) {
            view = layoutInflater.inflate(R.layout.recycler_chat_right, parent, false);
        } else if (viewType == ChatMessage.CHAT_TIME) {
            view = layoutInflater.inflate(R.layout.recycler_chat_time, parent, false);
        } else if (viewType == ChatMessage.FROM_USER_TO_GPT) {
            view = layoutInflater.inflate(R.layout.recycler_chat_right_gpt, parent, false);
        } else {
            view = layoutInflater.inflate(R.layout.recycler_chat_empty, parent, false);
        }
        return new TTsVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TTsVH holder, int position) {
        int type = getItemViewType(position);
        if (type == ChatMessage.CHAT_EMPTY) {
            holder.mTextView.setText(ChatMessage.getEmptyMessage().getContent());
        } else {
            ChatMessage chatMessage = mChatMessages.get(position);
            if (type == ChatMessage.FROM_NLP || type == ChatMessage.FROM_USER || type == ChatMessage.FROM_USER_TO_GPT) {
                holder.mTextView.setText(chatMessage.getContent().substring(0, Math.min(chatMessage.getContent().length(), 300)));
            } else if (type == ChatMessage.CHAT_TIME) {
                holder.mTextView.setText(chatMessage.formattedTime());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mChatMessages.size() > 0 ? mChatMessages.get(position).getFrom() : ChatMessage.CHAT_EMPTY;
    }

    @Override
    public int getItemCount() {
        return mChatMessages.size() > 0 ? mChatMessages.size() : 1;
    }

    static class TTsVH extends RecyclerView.ViewHolder {
        private TextView mTextView;

        TTsVH(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.tv_chat_message);
        }
    }
}
