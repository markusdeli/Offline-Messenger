package com.example.offlinemessenger.data;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.offlinemessenger.BluetoothConnectionService;
import com.example.offlinemessenger.db.AppDatabase;
import com.example.offlinemessenger.db.ChatMessageDao;
import com.example.offlinemessenger.db.ChatMessageEntity;
import com.example.offlinemessenger.db.ChatUserDao;
import com.example.offlinemessenger.db.ChatUserEntity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * View Model for the main (chat) activity.
 */
public class ChatViewModel extends ViewModel {

    private static final String TAG = "ChatViewModel";

    private BluetoothConnectionService mBtConnectionService;

    /** Handler for communication with the bluetooth transmission thread. */
    private Handler mBtHandler;

    /** A map of all chat messages from each individual user ID. */
    private final Map<UUID, List<ChatMessage>> mAllMessages = new HashMap<>();

    /** All messages from the currently displayed chat. */
    private MutableLiveData<List<ChatMessage>> mMessages = null;
    /** All users. */
    private MutableLiveData<List<ChatUser>> mUsers = null;
    /**
     * The chat user to display messages from.  If this is {@code null}, the
     * message list will contain all messages from all users.
     */
    private ChatUser mSelectedUser = null;

    /** The user DAO. */
    private ChatUserDao mUserDao;
    /** The message DAO. */
    private ChatMessageDao mMessageDao;

    /**
     * Build a new Chat View Model.
     *
     * @param database The app database.
     */
    ChatViewModel(AppDatabase database) {
        mBtHandler = new Handler(msg -> {
            if (msg.obj instanceof Sendable) {
                onSendableReceived((Sendable) msg.obj);
                return true;
            }

            return false;
        });
        mBtConnectionService = new BluetoothConnectionService(mBtHandler);
        mUserDao = database.getChatUserDao();
        mMessageDao = database.getChatMessageDao();
    }

    /**
     * Return a list of all users stored in the database.
     *
     * @return The user list.
     */
    public LiveData<List<ChatUser>> getChatUsers() {
        if (mUsers == null) {
            mUsers = new MutableLiveData<>();
            new UserFetchTask().execute();
        }

        return mUsers;
    }

    /**
     * Return a list of all messages stored in the database.
     *
     * @return The message list.
     */
    public LiveData<List<ChatMessage>> getChatMessages() {
        if (mMessages == null) {
            mMessages = new MutableLiveData<>();
            new MessageFetchTask().execute();
        }

        return mMessages;
    }

    /**
     * Add one or more users to the user list and cause all view components to update accordingly.
     *
     * @param users The new users.
     */
    public void addChatUser(ChatUser... users) {
        if (mUsers == null) {
            getChatUsers();
        }

        List<ChatUser> currentUsers = mUsers.getValue();
        if (currentUsers == null) {
            currentUsers = new LinkedList<>();
        }

        currentUsers.addAll(Arrays.asList(users));
        mUsers.setValue(currentUsers);
        new UserStoreTask().execute(users);
    }

    /**
     * Update the message list to only contain messages from the specified user.
     * This method MUST be called after {@link #getChatMessages()} because
     *
     * @param newUser The new user to display messages from.
     */
    public void changeChatUser(ChatUser newUser) {
        List<ChatMessage> messages = mAllMessages.get(newUser.getUUID());
        if (messages == null) {
            mMessages.setValue(new LinkedList<>());
        } else {
            mMessages.setValue(messages);
        }
    }

    public void removeChatUser(ChatUser... users) {
        if (mUsers == null) {
            getChatUsers();
        }

        List<ChatUser> currentUsers = mUsers.getValue();
        if (currentUsers == null) {
            currentUsers = new LinkedList<>();
        }

        currentUsers.removeAll(Arrays.asList(users));
        mUsers.setValue(currentUsers);
    }

    public void sendMessage(String content) {
        if (mBtConnectionService == null) {
            return;
        }
        ChatMessage msg = new ChatMessage(mSelectedUser.getUUID(), content, true);
        mBtConnectionService.write(msg);
        addChatMessages(msg);
    }

    /**
     * Add one or more messages to the message list and cause all view components to be updated
     * accordingly.
     *
     * @param msgs The new messages.
     */
    public void addChatMessages(ChatMessage... msgs) {
        if (mMessages == null) {
            getChatMessages();
        }

        List<ChatMessage> messages = mMessages.getValue();
        if (messages == null) {
            messages = new LinkedList<>();
        }

        messages.addAll(Arrays.asList(msgs));
        mMessages.setValue(messages);
        new MessageStoreTask().execute(msgs);
    }

    /**
     * Callback for any sendable entity that has been received.
     *
     * @param s The sendable.
     */
    private void onSendableReceived(Sendable s) {
        Log.d(TAG, "onSendableReceived: " + s.toString());
        if (s instanceof ChatUser) {
            onChatUserReceived((ChatUser) s);
        } else if (s instanceof ChatMessage) {
            onChatMessageReceived((ChatMessage) s);
        }
    }

    /**
     * Callback for all user objects that have been received.
     *
     * @param s
     */
    private void onChatUserReceived(ChatUser s) {
        switch (s.getAction()) {
            case ADD:
                Log.d(TAG, "onChatUserReceived, action ADD");
                s.setAction(Sendable.Action.NONE);
                addChatUser(s);
                break;

            case REMOVE:
                Log.d(TAG, "onChatUserReceived, action REMOVE");
                s.setAction(Sendable.Action.NONE);
                removeChatUser(s);
                break;
        }
    }

    /**
     * Callback when a new chat message has been received.
     *
     * @param m The message.
     */
    private void onChatMessageReceived(ChatMessage m) {
        m.setOwn(false);
        switch (m.getAction()) {
            case ADD:
            case NONE:
                Log.d(TAG, "onChatMessageReceived, action ADD");
                m.setAction(Sendable.Action.NONE);
                addChatMessages(m);
                break;

            case REMOVE:
                Log.d(TAG, "onChatMessageReceived, action REMOVE");
                m.setAction(Sendable.Action.NONE);
                // TODO: Add support for removing messages
                break;
        }
    }

    /**
     * Async Task for retrieving all users from the database
     * and converting them to data models usable by the UI.
     */
    @SuppressWarnings("StaticFieldLeak")
    private class UserFetchTask extends AsyncTask<Void, Void, List<ChatUser>> {

        @Override
        public List<ChatUser> doInBackground(Void... params) {
            List<ChatUser> users = new LinkedList<>();
            for (ChatUserEntity entity : mUserDao.getAll()) {
                users.add(new ChatUser(
                        Sendable.Action.NONE,
                        entity.name,
                        UUID.fromString(entity.id)
                ));
            }

            return users;
        }

        public void onPostExecute(List<ChatUser> users) {
            List<ChatUser> currentUsers = mUsers.getValue();
            if (currentUsers != null) {
                currentUsers.addAll(users);
            } else {
                currentUsers = users;
            }

            mUsers.setValue(currentUsers);
        }

    }

    /**
     * Async Task for retrieving all messages from the database
     * and converting them to data models usable by the UI.
     */
    @SuppressWarnings("StaticFieldLeak")
    private class MessageFetchTask extends AsyncTask<Void, Void, Map<UUID, List<ChatMessage>>> {

        @Override
        public Map<UUID, List<ChatMessage>> doInBackground(Void... params) {
            Map<UUID, List<ChatMessage>> allMessages = new HashMap<>();
            for (ChatMessageEntity entity : mMessageDao.getAll()) {
                UUID userId = UUID.fromString(entity.userId);
                List<ChatMessage> messages = allMessages.get(userId);
                if (messages == null) {
                    messages = new LinkedList<>();
                    allMessages.put(userId, messages);
                }

                messages.add(new ChatMessage(
                        Sendable.Action.ADD,
                        UUID.fromString(entity.userId),
                        entity.content,
                        entity.isOwn,
                        entity.timestamp,
                        UUID.fromString(entity.id)
                ));
            }

            return allMessages;
        }

        @Override
        public void onPostExecute(Map<UUID, List<ChatMessage>> data) {
            if (mSelectedUser == null) {
                mMessages.setValue(new LinkedList<>());
            } else {
                List<ChatMessage> messages = data.get(mSelectedUser.getUUID());
                if (messages == null) {
                    mMessages.setValue(new LinkedList<>());
                } else {
                    mMessages.setValue(messages);
                }
            }
        }

    }

    @SuppressWarnings("StaticFieldLeak")
    private class MessageStoreTask extends AsyncTask<ChatMessage, Void, Void> {

        @Override
        public Void doInBackground(ChatMessage... messages) {
            ChatMessageEntity[] entities = new ChatMessageEntity[messages.length];
            int i = 0;
            for (ChatMessage m : messages) {
                entities[i] = new ChatMessageEntity();
                entities[i].id = m.getUUID().toString();
                entities[i].userId = m.getSender().toString();
                entities[i].content = m.getContent();
                entities[i++].timestamp = m.getTimestamp();
            }

            mMessageDao.insertAll(entities);

            return null;
        }
    }

    @SuppressWarnings("StaticFieldLeak")
    private class UserStoreTask extends AsyncTask<ChatUser, Void, Void> {

        @Override
        public Void doInBackground(ChatUser... users) {
            ChatUserEntity[] entities = new ChatUserEntity[users.length];
            int i = 0;
            for (ChatUser m : users) {
                entities[i] = new ChatUserEntity();
                entities[i].id = m.getUUID().toString();
                entities[i++].name = m.getName();
            }

            mUserDao.insertAll(entities);

            return null;
        }
    }

}
