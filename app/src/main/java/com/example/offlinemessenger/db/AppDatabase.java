package com.example.offlinemessenger.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/**
 * The app's main database class containing both the messages and users table.
 */
@Database(entities = {ChatUserEntity.class, ChatMessageEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Get the DAO for the users table.
     *
     * @return The user DAO.
     */
    public abstract ChatUserDao getChatUserDao();

    /**
     * Get the DAO for the messages table.
     *
     * @return The message DAO.
     */
    public abstract ChatMessageDao getChatMessageDao();

}
