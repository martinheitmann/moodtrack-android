package com.app.moodtrack_android.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class StoredFile (
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val filename: String,
    val remoteId: String,
    val length: Int,
    val uploadDate: Date,
    val md5: String,
    val data: String
)