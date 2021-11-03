package com.example.offlinemessenger.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * Data model class for chat messages.
 */
public class ChatMessage extends Sendable {

    /** The sender UUID. */
    private final UUID mSender;
    /** The message content. */
    private final String mContent;
    /** The message timestamp. */
    private final long mTimestamp;
    /** The message id. */
    private final UUID mId;
    /** If {@code true}, the message was sent from this device. */
    private boolean mIsOwn;

    /**
     * Create a new chat message.
     *
     * @param sender    The message sender id.
     * @param content   The message content.
     * @param isOwn     If {@code true}, the message was sent from this device.
     */
    public ChatMessage(UUID sender, String content, boolean isOwn) {
        this(Action.ADD, sender, content, isOwn, System.currentTimeMillis(), UUID.randomUUID());
    }

    /**
     * Create a new chat message.
     *
     * @param action    The action (for transmitting to the other party).
     * @param sender    The message sender id.
     * @param content   The message content.
     * @param isOwn     If {@code true}, the message was sent from this device.
     */
    public ChatMessage(Sendable.Action action, UUID sender, String content, boolean isOwn) {
        this(action, sender, content, isOwn, System.currentTimeMillis(), UUID.randomUUID());
    }

    /**
     * Create a new chat message.
     *
     * @param action    The action (for transmitting to the other party).
     * @param sender    The message sender id.
     * @param content   The message content.
     * @param isOwn     If {@code true}, the message was sent from this device.
     * @param timestamp The point of time the message was sent.
     * @param id        The message id.
     */
    public ChatMessage(Sendable.Action action, UUID sender, String content, boolean isOwn,
                       long timestamp, UUID id) {
        super(action);
        mSender = sender;
        mContent = content;
        mTimestamp = timestamp;
        mId = id;
        mIsOwn = isOwn;
    }

    /**
     * Get the message content.
     *
     * @return The message content.
     */
    public String getContent() {
        return mContent;
    }

    /**
     * Get the message sender id.
     *
     * @return The sender id.
     */
    public UUID getSender() {
        return mSender;
    }

    /**
     * Get the UNIX timestamp in milliseconds this message was sent.
     *
     * @return The message time.
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Get the message id.
     *
     * @return The message id.
     */
    public UUID getUUID() {
        return mId;
    }

    /**
     * Get the message id's hash code (for use with the list adapter)
     *
     * @return The simple (int) message id.
     */
    public int getSimpleId() {
        return mId.hashCode();
    }

    /**
     * Return whether this message was sent from this device.
     *
     * @return {@code true} if the message was sent from this device.
     */
    public boolean isOwn() {
        return mIsOwn;
    }

    /**
     * Set whether this message is send from this device.
     *
     * @param isOwn {@code true} If the message was sent from this device.
     */
    public void setOwn(boolean isOwn) {
        mIsOwn = isOwn;
    }

    @NonNull
    @Override
    public String toString() {
        return mContent;
    }

    public static final Parcelable.Creator<ChatMessage> CREATOR =
            new Parcelable.Creator<ChatMessage>() {
                public ChatMessage createFromParcel(Parcel in) {
                    return (ChatMessage) in.readSerializable();
                }

                public ChatMessage[] newArray(int size) {
                    return new ChatMessage[size];
                }
            };

}
