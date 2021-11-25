package com.app.moodtrack_android.model.notificationquestionnaire

import java.io.Serializable

data class NQEdge(
    val _id: String,
    val nqId: String,
    val source: String,
    val target: String,
    val edgeLabel: String,
    val condition: NQCondition,
) : Serializable