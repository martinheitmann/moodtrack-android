package com.app.moodtrack_android.model.eventlog

import java.util.*

data class EventLog(
    val timestamp: Date,
    val actor: String,
    val action: String,
    val eventObject: String,
    val extras: List<EventLogExtra>,
)
