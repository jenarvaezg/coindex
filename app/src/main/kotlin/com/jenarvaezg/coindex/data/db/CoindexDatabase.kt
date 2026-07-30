package com.jenarvaezg.coindex.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CollectedItemEntity::class,
        TypeMetaEntity::class,
        ProposalPreferenceEntity::class,
        ApiCallEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class CoindexDatabase : RoomDatabase() {
    abstract fun collectedItems(): CollectedItemDao
    abstract fun typeMeta(): TypeMetaDao
    abstract fun proposalPreferences(): ProposalPreferenceDao
    abstract fun apiCalls(): ApiCallDao

    companion object {
        fun open(context: Context): CoindexDatabase =
            Room.databaseBuilder(context, CoindexDatabase::class.java, "coindex.db").build()
    }
}
