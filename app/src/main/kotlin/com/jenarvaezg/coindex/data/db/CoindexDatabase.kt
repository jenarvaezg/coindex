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
    ],
    version = 6,
    exportSchema = true,
)
abstract class CoindexDatabase : RoomDatabase() {
    abstract fun collectedItems(): CollectedItemDao
    abstract fun typeMeta(): TypeMetaDao
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

        /**
         * Version 4 puts the dominant metal into the variant key (#40, ADR 0018).
         *
         * The key *is* the primary key of `collection_proposal_preferences`, and SQLite cannot
         * add a column to one, so the table is rebuilt: renamed aside, recreated exactly as Room
         * declares it, and refilled row by row from the old one.
         *
         * Only rows this app can name a metal for are carried across, and that is a literal list
         * — [PRESERVED_KEYS], the thirty catalogs shipped at this version — rather than a lookup.
         * A migration is frozen history: reading today's `data/` inside it would silently change
         * what an old phone does the next time someone curates a catalog. Everything else is
         * dropped and comes back as **Disponible**, which is the price #55 already named for
         * touching a key: the two cupronickel cards of the father's Portuguese systems lose
         * their card and are re-followed by hand.
         */
        /** Every shipped catalog key at version 4: silver, and the 1983 set, which has no metal. */
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

        /**
         * The rebuilt table, verbatim as Room declares it. Same reason [VERSION_2_TABLES] is kept
         * as data: a keyword of drift here is a crash at open time on the collector's phone.
         */
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
         * Version 5 retires the dispositions (ADR 0021 §7): one `DROP`, forward only, rescuing
         * nothing.
         *
         * There is no data to save. The ~58 rows in the collector's phone are all `followed`
         * because following was the toll the plate charged, so they express that the app charged
         * it and not a preference — and ADR 0008 itself demanded a rollback drop the table rather
         * than reinterpret it. It is **irreversible on purpose**: if archiving a card ever earns
         * its case, the bit is rebuilt from zero rather than resurrected from these.
         *
         * `own_groupings` and `own_grouping_members` are untouched (ADR 0021 §11): a box is the one
         * thing the collector typed.
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
         * Version 6 gives the type cache the five fields that were being parsed out of the body on
         * every read (#221).
         *
         * Additive and nullable, like version 3 and for the same reason: the rows are filled in
         * afterwards by `FichaBackfill`, from the ficha each one already stores, because SQLite on
         * the oldest phone this app supports cannot be trusted to have `json_extract`.
         *
         * `readVersion` defaults to zero, which is «this row's columns were written by nobody» —
         * exactly what is true of every row on the other side of this migration, and what makes
         * the backfill find them.
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

        fun open(context: Context): CoindexDatabase =
            Room.databaseBuilder(context, CoindexDatabase::class.java, "coindex.db")
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                )
                .build()
    }
}
