package com.example.offlinemessenger.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Column definition for the users table.
 */
@Entity(tableName = "users")
public class ChatUserEntity {

    /** The user ID. */
    @PrimaryKey
    @NonNull
    public String id;

    /** The user name. */
    public String name;

}
