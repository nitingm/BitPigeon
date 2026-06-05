package com.codingskillshub.bitpigeon.domain.interfaces.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.codingskillshub.bitpigeon.domain.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertOrUpdateUser(user: User)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE id IN (:userIds)")
    fun getUsersByIds(userIds: List<String>): Flow<List<User>>

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("UPDATE users SET profilePicturePath = :fileName WHERE id = :userId")
    suspend fun updateProfilePicture(userId: String, fileName: String)

    @Query("UPDATE users SET name = :name WHERE id = :userId")
    suspend fun updateUserName(userId: String, name: String)
}