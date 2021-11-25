package com.app.moodtrack_android.model.notificationquestionnaire

import java.io.Serializable

data class NQNode(
    val _id: String,
    val nqId: String,
    val nodeLabel: String,
    val isSourceNode: Boolean,
    val data: NQData,
) : Serializable
