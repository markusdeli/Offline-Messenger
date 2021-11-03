package com.example.offlinemessenger;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.offlinemessenger.data.ChatMessage;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/**
 * Adapter class for the chat message list.
 */
public class ConversationListAdapter extends BaseAdapter {

    /** Our layout inflater. */
    private LayoutInflater inflater;
    /** The lists of messages. */
    private List<ChatMessage> messageList;
    /** Date format instance to avoid reinstanciation */
    private final DateFormat mDateFormat = DateFormat.getTimeInstance();

    /**
     * Create a new adapter.
     *
     * @param messageList The list of messages to display.
     * @param inflater    The layout inflater to use.
     */
    public ConversationListAdapter(List<ChatMessage> messageList, LayoutInflater inflater) {
        this.messageList = messageList;
        this.inflater = inflater;
    }

    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public ChatMessage getItem(int position) {
        return messageList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getSimpleId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.message_list_item, parent, false);
        }

        // TODO: Find a way to
        ChatMessage message = getItem(position);
        ((TextView) convertView.findViewById(R.id.message_item_content))
                .setText(message.getContent());
        ((TextView) convertView.findViewById(R.id.message_item_time))
                .setText(mDateFormat.format(new Date(message.getTimestamp())));

        return convertView;
    }

}
