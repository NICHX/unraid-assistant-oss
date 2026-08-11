package com.nichx.unraidassistant.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nichx.unraidassistant.core.db.dao.ServerDao
import com.nichx.unraidassistant.core.db.entity.ServerEntity

@Database(
    entities = [ServerEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class UnraidDatabase : RoomDatabase() {

    abstract fun serverDao(): ServerDao

    companion object {
        const val NAME = "unraid.db"
    }
}
