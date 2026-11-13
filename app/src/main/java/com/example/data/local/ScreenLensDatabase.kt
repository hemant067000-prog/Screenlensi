package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "glossary_entries")
data class GlossaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourcePhrase: String,
    val englishTranslation: String,
    val languageCode: String = "AUTO",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subtitle_records")
data class SubtitleRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sequenceNumber: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val detectedLangCode: String,
    val detectedLangName: String,
    val sourceText: String,
    val translatedText: String,
    val latencyMs: Double,
    val regionTag: String,
    val wasOffScreen: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ScreenLensDao {
    @Query("SELECT * FROM glossary_entries ORDER BY createdAt DESC")
    fun observeGlossary(): Flow<List<GlossaryEntryEntity>>

    @Query("SELECT COUNT(*) FROM glossary_entries")
    suspend fun getGlossaryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlossaryEntry(entry: GlossaryEntryEntity)

    @Query("DELETE FROM glossary_entries WHERE id = :id")
    suspend fun deleteGlossaryEntry(id: Long)

    @Query("SELECT * FROM subtitle_records ORDER BY createdAt DESC LIMIT 200")
    fun observeSubtitles(): Flow<List<SubtitleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(record: SubtitleRecordEntity)

    @Query("DELETE FROM subtitle_records")
    suspend fun clearSubtitles()
}

@Database(
    entities = [GlossaryEntryEntity::class, SubtitleRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ScreenLensDatabase : RoomDatabase() {
    abstract fun dao(): ScreenLensDao

    companion object {
        @Volatile
        private var INSTANCE: ScreenLensDatabase? = null

        fun getInstance(context: Context): ScreenLensDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScreenLensDatabase::class.java,
                    "screenlens_offline.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}

class ScreenLensRepository(private val dao: ScreenLensDao) {
    val glossaryFlow: Flow<List<GlossaryEntryEntity>> = dao.observeGlossary()
    val subtitlesFlow: Flow<List<SubtitleRecordEntity>> = dao.observeSubtitles()

    suspend fun ensureDefaultGlossarySeeded() {
        if (dao.getGlossaryCount() == 0) {
            val defaults = listOf(
                GlossaryEntryEntity(
                    sourcePhrase = "領域展開",
                    englishTranslation = "Domain Expansion [Glossary Override]",
                    languageCode = "JA"
                ),
                GlossaryEntryEntity(
                    sourcePhrase = "마왕성 결전",
                    englishTranslation = "Demon King Castle Final Battle [Glossary]",
                    languageCode = "KO"
                ),
                GlossaryEntryEntity(
                    sourcePhrase = "量子核心已就绪",
                    englishTranslation = "Quantum Core Online [Glossary]",
                    languageCode = "ZH"
                ),
                GlossaryEntryEntity(
                    sourcePhrase = "सुपरफास्ट बुलेटिन",
                    englishTranslation = "Superfast News Bulletin [Glossary]",
                    languageCode = "HI"
                ),
                GlossaryEntryEntity(
                    sourcePhrase = "थेट प्रक्षेपण",
                    englishTranslation = "Live Broadcast Feed [Glossary]",
                    languageCode = "MR"
                )
            )
            defaults.forEach { dao.insertGlossaryEntry(it) }
        }
    }

    suspend fun addGlossaryEntry(source: String, english: String, langCode: String = "AUTO") {
        if (source.isBlank() || english.isBlank()) return
        dao.insertGlossaryEntry(
            GlossaryEntryEntity(
                sourcePhrase = source.trim(),
                englishTranslation = english.trim(),
                languageCode = langCode.uppercase()
            )
        )
    }

    suspend fun deleteGlossaryEntry(id: Long) {
        dao.deleteGlossaryEntry(id)
    }

    suspend fun recordSubtitle(record: SubtitleRecordEntity) {
        dao.insertSubtitle(record)
    }

    suspend fun clearSubtitleHistory() {
        dao.clearSubtitles()
    }

    fun formatAsTxt(records: List<SubtitleRecordEntity>): String {
        val sorted = records.sortedBy { it.sequenceNumber }
        if (sorted.isEmpty()) return "# ScreenLens Background — Empty Transcript"
        val sb = StringBuilder()
        sb.appendLine("# ScreenLens Background — 100% Offline Translation Transcript")
        sb.appendLine("# Generated locally with Zero Cloud API calls")
        sb.appendLine("------------------------------------------------------------")
        for (item in sorted) {
            val timeStr = formatClockTime(item.startTimeMs)
            val modeTag = if (item.wasOffScreen) "[OFF-SCREEN]" else "[FOCUSED]"
            sb.appendLine("[$timeStr] [${item.detectedLangCode}] [${item.regionTag}] $modeTag (${String.format("%.1f", item.latencyMs)}ms)")
            sb.appendLine("  SRC: ${item.sourceText}")
            sb.appendLine("  ENG: ${item.translatedText}")
            sb.appendLine()
        }
        return sb.toString()
    }

    fun formatAsSrt(records: List<SubtitleRecordEntity>): String {
        val sorted = records.sortedBy { it.sequenceNumber }
        if (sorted.isEmpty()) {
            return "1\n00:00:00,000 --> 00:00:02,500\n[ScreenLens Offline] No subtitles recorded yet.\n"
        }
        val sb = StringBuilder()
        sorted.forEachIndexed { idx, item ->
            val seq = idx + 1
            val start = formatSrtTimestamp(item.startTimeMs)
            val end = formatSrtTimestamp(item.endTimeMs.coerceAtLeast(item.startTimeMs + 2200L))
            sb.appendLine(seq)
            sb.appendLine("$start --> $end")
            sb.appendLine("[${item.detectedLangCode}] ${item.translatedText}")
            sb.appendLine(item.sourceText)
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun formatClockTime(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val mins = (totalSec / 60L) % 60L
        val secs = totalSec % 60L
        val millis = (ms % 1000L) / 10L
        return String.format("%02d:%02d.%02d", mins, secs, millis)
    }

    private fun formatSrtTimestamp(ms: Long): String {
        val safeMs = ms.coerceAtLeast(0L)
        val hours = safeMs / 3_600_000L
        val minutes = (safeMs % 3_600_000L) / 60_000L
        val seconds = (safeMs % 60_000L) / 1_000L
        val millis = safeMs % 1_000L
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }
}
