package com.example.offlinemessenger.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Column definition for the messages table.
 */
@Entity(
        foreignKeys = @ForeignKey(
                entity = ChatUserEntity.class,
                parentColumns = "id",
                childColumns = "user_id"
        ),
        indices = @Index(name = "messages_user_id", value = "user_id"),
        tableName = "messages"
)
public class ChatMessageEntity {

    /** The message id. */
    @PrimaryKey
    @NonNull
    public String id;

    /** The user ID this message belongs to. */
    @ColumnInfo(name = "user_id")
    public String userId;

    /** The message content. */
    public String content;

    /** The UNIX timestamp in milliseconds this message was sent or received. */
    public long timestamp;

    /** If {@code true}, the message was sent from this device. */
    @ColumnInfo(name = "is_own")
    public boolean isOwn;

}
