package kr.co.clipnote.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalClipDao {
    @Query("SELECT * FROM local_clips ORDER BY savedAt DESC")
    suspend fun all(): List<LocalClipEntity>

    @Query("SELECT COUNT(*) FROM local_clips")
    suspend fun count(): Int

    @Query("SELECT * FROM local_clips WHERE url = :url")
    suspend fun find(url: String): LocalClipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(clip: LocalClipEntity)

    @Query("DELETE FROM local_clips WHERE url = :url")
    suspend fun delete(url: String)

    @Query("DELETE FROM local_clips")
    suspend fun clear()

    /** 상한을 넘긴 만큼 오래된 것부터 지운다. */
    @Query("DELETE FROM local_clips WHERE url IN (SELECT url FROM local_clips ORDER BY savedAt DESC LIMIT -1 OFFSET :keep)")
    suspend fun trimTo(keep: Int)
}
