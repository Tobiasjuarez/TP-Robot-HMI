package ar.edu.uade.robothmi.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import ar.edu.uade.robothmi.domain.model.EventCategory
import ar.edu.uade.robothmi.domain.model.HistoryEntry
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val category: String,
    val title: String,
    val detail: String?,
)

@Dao
interface EventLogDao {
    @Insert
    suspend fun insert(entry: EventLogEntity)

    @Query("SELECT * FROM event_log ORDER BY timestampMs DESC, id DESC LIMIT 500")
    fun observeAll(): Flow<List<EventLogEntity>>

    @Query("DELETE FROM event_log")
    suspend fun clear()
}

fun EventLogEntity.toDomain() = HistoryEntry(
    id = id,
    timestampMs = timestampMs,
    category = runCatching { EventCategory.valueOf(category) }.getOrDefault(EventCategory.SISTEMA),
    title = title,
    detail = detail,
)
