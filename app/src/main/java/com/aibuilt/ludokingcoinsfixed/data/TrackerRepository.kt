package com.aibuilt.ludokingcoinsfixed.data

import android.content.Context
import com.aibuilt.ludokingcoinsfixed.model.LedgerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class LedgerFile(
    val balance: Long = 0L,
    val entries: List<LedgerEntry> = emptyList()
)

class TrackerRepository(context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(context.filesDir, "coin_tracker.json")

    suspend fun load(): LedgerFile = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) LedgerFile() else json.decodeFromString(LedgerFile.serializer(), file.readText())
        }.getOrElse { LedgerFile() }
    }

    suspend fun save(data: LedgerFile): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            file.writeText(json.encodeToString(LedgerFile.serializer(), data))
        }.isSuccess
    }
}
