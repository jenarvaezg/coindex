package com.jenarvaezg.coindex.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Database(
    entities = [
        CollectedItemEntity::class,
        TypeMetaEntity::class,
        ProposalPreferenceEntity::class,
        OwnGroupingEntity::class,
        OwnGroupingMemberEntity::class,
        ApiCallEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class CoindexDatabase : RoomDatabase() {
    abstract fun collectedItems(): CollectedItemDao
    abstract fun typeMeta(): TypeMetaDao
    abstract fun proposalPreferences(): ProposalPreferenceDao
    abstract fun ownGroupings(): OwnGroupingDao
    abstract fun apiCalls(): ApiCallDao

    companion object {
        /**
         * Version 2 adds the collector's own groupings (ADR 0013) and touches nothing else.
         *
         * Explicitly, never destructively: on the other side of this migration there is a
         * synced collection that cost API budget to fetch and a type cache that is never
         * refetched. Dropping it to add two tables would be trading the data for the feature.
         */
        /**
         * The two tables version 2 adds, verbatim as Room declares them.
         *
         * Kept as data rather than inline so a unit test can compare them against the schema
         * Room exports from the entities: hand-written migration SQL that drifts from the
         * entity by one keyword fails at runtime on the collector's phone, at which point the
         * only remaining move is the destructive one.
         */
        internal val VERSION_2_TABLES: List<String> = listOf(
            "CREATE TABLE IF NOT EXISTS `own_groupings` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `own_grouping_members` " +
                "(`groupingId` INTEGER NOT NULL, `typeId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`groupingId`, `typeId`), " +
                "FOREIGN KEY(`groupingId`) REFERENCES `own_groupings`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )

        /**
         * Version 3 gives the type cache the thumbnail of each face (issue #67).
         *
         * Two nullable columns and nothing else: the rows themselves are filled in afterwards
         * by `TypeThumbnailBackfill`, from the ficha each one already stores, because SQLite
         * on the oldest phone this app supports cannot be trusted to have `json_extract`.
         *
         * Room compares the migrated table against the exported schema by column name, not by
         * declaration order, so appended columns match an entity that declares them anywhere.
         */
        internal val VERSION_3_COLUMNS: List<String> = listOf(
            "ALTER TABLE `type_meta` ADD COLUMN `obverseThumbnailUrl` TEXT",
            "ALTER TABLE `type_meta` ADD COLUMN `reverseThumbnailUrl` TEXT",
        )

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                VERSION_2_TABLES.forEach(connection::execSQL)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                VERSION_3_COLUMNS.forEach(connection::execSQL)
            }
        }

        fun open(context: Context): CoindexDatabase =
            Room.databaseBuilder(context, CoindexDatabase::class.java, "coindex.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
