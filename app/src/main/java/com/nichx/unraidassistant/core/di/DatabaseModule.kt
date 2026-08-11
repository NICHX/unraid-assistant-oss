package com.nichx.unraidassistant.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nichx.unraidassistant.core.db.UnraidDatabase
import com.nichx.unraidassistant.core.db.dao.ServerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** v1 → v2：servers 表新增 insecureSkipVerify（跳过证书校验）列，默认关闭。 */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE servers ADD COLUMN insecureSkipVerify INTEGER NOT NULL DEFAULT 0",
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UnraidDatabase =
        Room.databaseBuilder(context, UnraidDatabase::class.java, UnraidDatabase.NAME)
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideServerDao(db: UnraidDatabase): ServerDao = db.serverDao()
}
