package com.mg.costeoapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val tableName: String,

    @ColumnInfo(name = "last_push_at")
    val lastPushAt: Long = 0,

    @ColumnInfo(name = "last_pull_at")
    val lastPullAt: Long = 0
)
