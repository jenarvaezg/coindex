package com.jenarvaezg.coindex.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** One disposition key the version 4 migration carries across, and the metal it is given. */
internal data class PreservedKey(
    val family: String,
    val weightMillioz: Int,
    val finishCode: String,
    val metalCode: String,
)

@Database(
    entities = [
        CollectedItemEntity::class,
        TypeMetaEntity::class,
        OwnGroupingEntity::class,
        OwnGroupingMemberEntity::class,
        ApiCallEntity::class,
        IssuePriceReadEntity::class,
        IssuePriceEntity::class,
        MetalSpotEntity::class,
        TypeIssueReadEntity::class,
        TypeIssueEntity::class,
        WishEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class CoindexDatabase : RoomDatabase() {
    abstract fun collectedItems(): CollectedItemDao
    abstract fun typeMeta(): TypeMetaDao
    abstract fun ownGroupings(): OwnGroupingDao
    abstract fun apiCalls(): ApiCallDao
    abstract fun prices(): PriceDao
    abstract fun wishes(): WishDao

    /**
     * Folds the write-ahead log into [DATABASE_NAME] so one file holds every transaction (#548):
     * Room runs in WAL mode (hence the three files `scripts/avd-db.sh` copies), and an export
     * through the share sheet is a single file. `TRUNCATE` rather than `PASSIVE`, which gives up
     * silently when a reader is in the way.
     *
     * `PRAGMA wal_checkpoint` never throws: a sync, the ledger or the prefetch in the way comes back
     * as `busy = 1` with the log untouched, and ignoring it would export a dump missing the coins a
     * sync just added. So it fails loudly and the collector taps again.
     */
    fun checkpoint() {
        openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { row ->
            check(row.moveToFirst()) { "SQLite no contestó al plegar el diario" }
            // Shown to the collector after «No se pudieron exportar los datos: …», so it says what
            // to do.
            check(row.getInt(0) == 0) { "la base estaba en uso, inténtalo otra vez" }
        }
    }

    companion object {
        /** The one file the collection lives in, on the phone and in the vault of `avd-db.sh`. */
        const val DATABASE_NAME: String = "coindex.db"

        /**
         * The two tables version 2 adds for the collector's groupings (ADR 0021 §11), verbatim as Room
         * declares them. Migrations are explicit, never destructive: the synced collection and the
         * type cache cost API budget. Kept as data so a unit test compares them against Room's
         * exported schema; SQL that drifts from the entity fails at open time on the collector's
         * phone.
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
         * Version 3 adds each face's thumbnail to the type cache (#67). `TypeThumbnailBackfill`
         * fills them from the stored ficha afterwards, because SQLite on the oldest supported phone
         * may lack `json_extract`. Room matches migrated columns by name, so appending them is fine.
         */
        internal val VERSION_3_COLUMNS: List<String> = listOf(
            "ALTER TABLE `type_meta` ADD COLUMN `obverseThumbnailUrl` TEXT",
            "ALTER TABLE `type_meta` ADD COLUMN `reverseThumbnailUrl` TEXT",
        )

        /**
         * Version 4 puts the dominant metal into the variant key (#40, ADR 0018). The key is the
         * primary key of `collection_proposal_preferences`, so the table is rebuilt: renamed aside,
         * recreated as Room declares it and refilled from the old one.
         *
         * Only the keys of the catalogs shipped at this version are carried over, as a literal
         * list: a migration is frozen history and must not read today's `data/`. Everything else
         * comes back as Disponible (#55). The 1983 set has no metal.
         */
        internal val PRESERVED_KEYS: List<PreservedKey> = listOf(
            PreservedKey("Architectural Monuments of Russia", 1_121, "unknown", "silver"),
            PreservedKey("Australian Koala", 1_000, "unknown", "silver"),
            PreservedKey("Australian Kookaburra", 1_000, "unknown", "silver"),
            PreservedKey("Dólar conmemorativo de plata .500 de Canadá", 750, "unknown", "silver"),
            PreservedKey("Dólar de plata .800 de Canadá", 750, "unknown", "silver"),
            PreservedKey("Equilibrium", 1_000, "unknown", "silver"),
            PreservedKey("Capitales de provincia y ciudades autónomas", 434, "proof", "silver"),
            PreservedKey("100 Pesetas de Franco", 611, "unknown", "silver"),
            PreservedKey("The Lion and the Eagle", 1_000, "bullion", "silver"),
            PreservedKey("Lunar Series II", 1_000, "bullion", "silver"),
            PreservedKey("Lunar Series III", 1_000, "bullion", "silver"),
            PreservedKey("Nikola Tesla", 1_000, "unknown", "silver"),
            PreservedKey("Outstanding Personalities of Russia", 547, "unknown", "silver"),
            PreservedKey("10 gulden conmemorativos de Beatrix", 482, "unknown", "silver"),
            PreservedKey(
                "1000 escudos conmemorativos de plata .500 de Portugal",
                900,
                "unknown",
                "silver",
            ),
            PreservedKey("XVII Exposición Europea de Arte de 1983", -1, "unknown", "unknown"),
            PreservedKey(
                "500 escudos conmemorativos de plata .500 de Portugal",
                450,
                "unknown",
                "silver",
            ),
            PreservedKey("The Queen's Beasts", 2_000, "unknown", "silver"),
            PreservedKey("Red Data Book", 547, "unknown", "silver"),
            PreservedKey("Lunar ounce", 1_000, "unknown", "silver"),
            PreservedKey("Nautical Ounce", 1_000, "unknown", "silver"),
            PreservedKey("Australian Saltwater Crocodile", 1_000, "unknown", "silver"),
            PreservedKey(
                "Serie de monedas de plata obtenidas a valor facial",
                579,
                "unknown",
                "silver",
            ),
            PreservedKey("St George and the Dragon", 1_000, "bullion", "silver"),
            PreservedKey("The Royal Tudor Beasts", 1_000, "proof", "silver"),
            PreservedKey("The Royal Tudor Beasts", 2_000, "bullion", "silver"),
            PreservedKey(
                "500th Anniversary of the United Russian State",
                1_111,
                "unknown",
                "silver",
            ),
            PreservedKey(
                "250th anniversary of the United States Declaration of Independence",
                868,
                "unknown",
                "silver",
            ),
            PreservedKey("2 Bolívares de Venezuela", 322, "unknown", "silver"),
            PreservedKey("Fuertes de Venezuela", 804, "unknown", "silver"),
        )

        private const val PREFERENCES_BACKUP = "collection_proposal_preferences_pre_v4"

        /** The rebuilt table, verbatim as Room declares it; kept as data like [VERSION_2_TABLES]. */
        internal val VERSION_4_PREFERENCES_TABLE: String =
            "CREATE TABLE IF NOT EXISTS `collection_proposal_preferences` " +
                "(`family` TEXT NOT NULL, `weightMillioz` INTEGER NOT NULL, " +
                "`finishCode` TEXT NOT NULL, `metalCode` TEXT NOT NULL, " +
                "`disposition` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`family`, `weightMillioz`, `finishCode`, `metalCode`))"

        /** Parameterized, so a family holding a quote — «The Queen's Beasts» — needs no escaping. */
        internal val VERSION_4_CARRY_OVER: String =
            "INSERT INTO `collection_proposal_preferences` " +
                "(`family`, `weightMillioz`, `finishCode`, `metalCode`, " +
                "`disposition`, `createdAt`, `updatedAt`) " +
                "SELECT `family`, `weightMillioz`, `finishCode`, ?, " +
                "`disposition`, `createdAt`, `updatedAt` " +
                "FROM `$PREFERENCES_BACKUP` " +
                "WHERE `family` = ? AND `weightMillioz` = ? AND `finishCode` = ?"

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

        /**
         * Version 5 retires the dispositions (ADR 0021 §7) with one irreversible `DROP`. Nothing is
         * rescued: every row was `followed`, the toll the plate used to charge rather than a
         * preference, and ADR 0008 asked for a rollback to drop the table, not reinterpret it. The
         * groupings stay (ADR 0021 §11).
         */
        internal const val VERSION_5_DROP: String =
            "DROP TABLE `collection_proposal_preferences`"

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(VERSION_5_DROP)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `collection_proposal_preferences` " +
                        "RENAME TO `$PREFERENCES_BACKUP`",
                )
                connection.execSQL(VERSION_4_PREFERENCES_TABLE)
                val statement = connection.prepare(VERSION_4_CARRY_OVER)
                try {
                    for (key in PRESERVED_KEYS) {
                        statement.reset()
                        statement.bindText(1, key.metalCode)
                        statement.bindText(2, key.family)
                        statement.bindLong(3, key.weightMillioz.toLong())
                        statement.bindText(4, key.finishCode)
                        statement.step()
                    }
                } finally {
                    statement.close()
                }
                connection.execSQL("DROP TABLE `$PREFERENCES_BACKUP`")
            }
        }

        /**
         * Version 6 stores five fields that were parsed out of the body on every read (#221).
         * Additive and nullable like version 3, and filled by `FichaBackfill` for the same reason.
         * `readVersion` defaults to 0, «not filled yet», which is how the backfill finds every
         * migrated row.
         */
        internal val VERSION_6_COLUMNS: List<String> = listOf(
            "ALTER TABLE `type_meta` ADD COLUMN `issuerName` TEXT",
            "ALTER TABLE `type_meta` ADD COLUMN `composition` TEXT",
            "ALTER TABLE `type_meta` ADD COLUMN `sizeMillimetres` REAL",
            "ALTER TABLE `type_meta` ADD COLUMN `category` TEXT",
            "ALTER TABLE `type_meta` ADD COLUMN `numistaUrl` TEXT",
            "ALTER TABLE `type_meta` ADD COLUMN `readVersion` INTEGER NOT NULL DEFAULT 0",
        )

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                VERSION_6_COLUMNS.forEach(connection::execSQL)
            }
        }

        /**
         * Version 7 adds money (ADR 0028), all additive: prices per grade, the reads that tell
         * «asked and empty» from «not asked», the last spot, and four type-cache columns that
         * `FichaBackfill` fills from the stored fichas. No API call; a collected item's `issue_id`
         * is already read from its stored body (#327). Kept as data like [VERSION_2_TABLES].
         */
        internal val VERSION_7_TABLES: List<String> = listOf(
            "CREATE TABLE IF NOT EXISTS `issue_price_reads` " +
                "(`typeId` INTEGER NOT NULL, `issueId` INTEGER NOT NULL, " +
                "`readAt` INTEGER NOT NULL, `hasPrices` INTEGER NOT NULL, " +
                "PRIMARY KEY(`typeId`, `issueId`))",
            "CREATE TABLE IF NOT EXISTS `issue_prices` " +
                "(`typeId` INTEGER NOT NULL, `issueId` INTEGER NOT NULL, `grade` TEXT NOT NULL, " +
                "`eur` REAL NOT NULL, PRIMARY KEY(`typeId`, `issueId`, `grade`))",
            "CREATE TABLE IF NOT EXISTS `metal_spot` " +
                "(`symbol` TEXT NOT NULL, `eurPerTroyOunce` REAL NOT NULL, " +
                "`readAt` INTEGER NOT NULL, PRIMARY KEY(`symbol`))",
        )

        internal val VERSION_7_COLUMNS: List<String> = listOf(
            "ALTER TABLE `type_meta` ADD COLUMN `thicknessMillimetres` REAL",
            "ALTER TABLE `type_meta` ADD COLUMN `demonetized` INTEGER",
            "ALTER TABLE `type_meta` ADD COLUMN `hands` TEXT",
            "ALTER TABLE `type_meta` ADD COLUMN `mints` TEXT",
        )

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(connection: SQLiteConnection) {
                VERSION_7_TABLES.forEach(connection::execSQL)
                VERSION_7_COLUMNS.forEach(connection::execSQL)
            }
        }

        /**
         * Version 8 stores the issue listings (#452): two new tables, nothing rewritten, so the
         * first pass after the update lists the types once. Kept as data like [VERSION_2_TABLES].
         */
        internal val VERSION_8_TABLES: List<String> = listOf(
            "CREATE TABLE IF NOT EXISTS `type_issue_reads` " +
                "(`typeId` INTEGER NOT NULL, `readAt` INTEGER NOT NULL, PRIMARY KEY(`typeId`))",
            "CREATE TABLE IF NOT EXISTS `type_issues` " +
                "(`typeId` INTEGER NOT NULL, `issueId` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
                "`year` INTEGER, `gregorianYear` INTEGER, PRIMARY KEY(`typeId`, `issueId`))",
        )

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(connection: SQLiteConnection) {
                VERSION_8_TABLES.forEach(connection::execSQL)
            }
        }

        /**
         * Version 9 gives a medal's issue year its own column (#460), filled by `FichaBackfill` from
         * the stored fichas like versions 3, 6 and 7.
         */
        internal val VERSION_9_COLUMNS: List<String> = listOf(
            "ALTER TABLE `type_meta` ADD COLUMN `issuedYear` INTEGER",
        )

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(connection: SQLiteConnection) {
                VERSION_9_COLUMNS.forEach(connection::execSQL)
            }
        }

        /**
         * Version 10 adds the marked casillas (ADR 0029, #497). Not the dispositions coming back: a
         * wish is keyed by casilla rather than variant, and stops counting once the slot is filled
         * (ADR 0029 §2). Kept as data like [VERSION_2_TABLES].
         */
        internal val VERSION_10_TABLES: List<String> = listOf(
            "CREATE TABLE IF NOT EXISTS `wishes` " +
                "(`typeId` INTEGER NOT NULL, `year` INTEGER NOT NULL, " +
                "`issueId` INTEGER NOT NULL, `markedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`typeId`, `year`, `issueId`))",
        )

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
                VERSION_10_TABLES.forEach(connection::execSQL)
            }
        }

        fun open(context: Context): CoindexDatabase =
            Room.databaseBuilder(context, CoindexDatabase::class.java, DATABASE_NAME)
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                )
                .build()
    }
}
