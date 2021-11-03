package com.example.offlinemessenger.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO for the messages table.
 */
@Dao
public interface ChatMessageDao {

    /**
     * Insert a single chat message.
     *
     * @param entity The chat message DB entity.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ChatMessageEntity entity);

    /**
     * Insert multiple chat messages.
     *
     * @param entities The chat messages.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(ChatMessageEntity... entities);

    /**
     * Get a list of all chat messages.
     *
     * TODO: Add pagination so we don't flood the entire RAM when there are
     *       several million messages.
     *
     * @return The list of all chat messages, ordered by time.
     */
    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    List<ChatMessageEntity> getAll();

    /**
     * Get a list of all chat messages from a specific user.
     *
     * @param userId The user id.
     * @return The chat message list.
     */
    @Query("SELECT * FROM messages WHERE user_id = :userId ORDER BY TIMESTAMP ASC")
    List<ChatMessageEntity> getAllFromUser(String userId);

    /**
     * Delete a single chat message.
     *
     * @param entity The message to delete.
     */
    @Delete
    void delete(ChatMessageEntity entity);

}
