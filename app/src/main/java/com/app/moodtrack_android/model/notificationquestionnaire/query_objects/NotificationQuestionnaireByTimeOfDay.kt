package com.app.moodtrack_android.model.notificationquestionnaire.query_objects

import com.app.moodtrack_android.model.notificationquestionnaire.NQEdge
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import java.io.Serializable

data class NotificationQuestionnaireByTimeOfDay(
    val nqId: String,
    val nodes: List<NQNode>,
    val edges: List<NQEdge>
) : Serializable
