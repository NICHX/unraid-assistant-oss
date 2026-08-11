package com.nichx.unraidassistant.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.nichx.unraidassistant.core.db.entity.ServerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: String): ServerEntity?

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(entity: ServerEntity)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun delete(id: String)
}
