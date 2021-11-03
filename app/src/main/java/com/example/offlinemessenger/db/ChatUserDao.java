package com.example.offlinemessenger.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO for the users table.
 */
@Dao
public interface ChatUserDao {

    /**
     * Insert a single user into the database.
     *
     * @param entity The user entity.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(ChatUserEntity entity);

    /**
     * Insert multiple users into the database,
     *
     * @param entities The user entities.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(ChatUserEntity... entities);

    /**
     * Get a list of all users stored in the database.
     *
     * @return The lit of all users.
     */
    @Query("SELECT * FROM users")
    List<ChatUserEntity> getAll();

    /**
     * Get a user by their user ID.
     *
     * @param id The user ID.
     * @return The user.
     */
    @Query("SELECT DISTINCT * FROM users WHERE id = :id")
    ChatUserEntity getById(String id);

    /**
     * Delete a user from the database.
     *
     * @param entity The user to delete.
     */
    @Delete
    void delete(ChatUserEntity entity);

}
