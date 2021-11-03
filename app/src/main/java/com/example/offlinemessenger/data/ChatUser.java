package com.example.offlinemessenger.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * Data model class for chat users.
 */
public class ChatUser extends Sendable {

    /** The user name. */
    private final String mName;
    /** The user id. */
    private final UUID mUUID;

    /**
     * Create a new chat user.
     * A new user id will be generated automatically.
     *
     * @param action The action (for transmitting to the other party).
     * @param name   The user name.
     */
    public ChatUser(Action action, String name) {
        this(action, name, UUID.randomUUID());
    }

    /**
     * Create a new chat user.
     *
     * @param action The action (for transmitting to the other party).
     * @param name   The user name.
     * @param uuid   The user id.
     */
    public ChatUser(Action action, String name, UUID uuid) {
        super(action);
        mName = name;
        mUUID = uuid;
    }

    /**
     * Get the user name.
     *
     * @return The user name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the user id.
     *
     * @return The user id.
     */
    public UUID getUUID() {
        return mUUID;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChatUser)) {
            return false;
        }

        return ((ChatUser) o).getUUID().equals(mUUID);
    }

    @NonNull
    @Override
    public String toString() {
        return mName;
    }

    public static final Parcelable.Creator<ChatUser> CREATOR =
            new Parcelable.Creator<ChatUser>() {
                public ChatUser createFromParcel(Parcel in) {
                    return (ChatUser) in.readSerializable();
                }

                public ChatUser[] newArray(int size) {
                    return new ChatUser[size];
                }
            };

}
