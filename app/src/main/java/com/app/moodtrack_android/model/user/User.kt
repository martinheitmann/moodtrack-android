package com.app.moodtrack_android.model.user

import java.io.Serializable
import java.util.*

data class User(
    val _id: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val age: Int? = null,
    val fcmRegistrationToken: String? = null,
    val notificationsEnabled: Boolean? = null,
    val profileImage: String? = null,
    val creationDate: Date? = null
) : Serializable