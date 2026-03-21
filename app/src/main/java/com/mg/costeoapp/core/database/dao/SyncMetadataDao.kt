package com.mg.costeoapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mg.costeoapp.core.database.entity.SyncMetadata

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE tableName = :table")
    suspend fun get(table: String): SyncMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadata)
}
