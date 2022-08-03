package com.app.moodtrack_android.model

import java.util.*

data class DocumentFile (
    val id: String,
    val filename: String,
    val length: Int,
    val uploadDate: Date,
    val md5: String,
    val data: String?,
    val ownerId: String
)