package com.app.moodtrack_android.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.IconsByIdQuery
import com.app.moodtrack_android.IconsByNameQuery
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.StoredFile
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphQLFileDao @Inject constructor(
    private val client: ApolloClient,
    private val tokenResolver: FirebaseAuthIdTokenResolver
) {

    suspend fun queryFiles(filenames: List<String>): List<StoredFile> {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response = client.query(IconsByNameQuery(Input.fromNullable(filenames)))
                .toBuilder()
                .requestHeaders(
                    RequestHeaders
                        .builder()
                        .addHeader("Authorization", idToken)
                        .build()
                )
                .build()
                .await()
            if (!response.hasErrors()) {
                val data = response.data
                if (data?.iconsByName != null) {
                    val iconData = data.iconsByName
                    if (iconData.isNotEmpty()) {
                        val result = mutableListOf<StoredFile>()
                        iconData.forEach { data ->
                            val filename = data?.filename
                            val remoteId = data?._id
                            val length = data?.length
                            val uploadDate = data?.uploadDate as String?
                            val md5 = data?.md5
                            val filedata = data?.data
                            if (filename != null && remoteId != null && length != null && uploadDate != null
                                && md5 != null && filedata != null
                            ) {
                                result.add(
                                    StoredFile(
                                        filename = filename,
                                        remoteId = remoteId,
                                        length = length,
                                        uploadDate = parseDate(uploadDate),
                                        md5 = md5,
                                        data = filedata
                                    )
                                )
                            } else {
                                throw IOException("Received response contained one or more null fields")
                            }
                        }
                        return result
                    } else {
                        return listOf()
                    }
                } else {
                    throw IOException("Received response contained no data or user")
                }
            } else {
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun queryFilesById(fileIds: List<String>): List<StoredFile> {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response = client.query(IconsByIdQuery(Input.fromNullable(fileIds)))
                .toBuilder()
                .requestHeaders(
                    RequestHeaders
                        .builder()
                        .addHeader("Authorization", idToken)
                        .build()
                )
                .build()
                .await()
            if (!response.hasErrors()) {
                val data = response.data
                if (data?.iconsById != null) {
                    val iconData = data.iconsById
                    if (iconData.isNotEmpty()) {
                        val result = mutableListOf<StoredFile>()
                        iconData.forEach { data ->
                            val filename = data?.filename
                            val remoteId = data?._id
                            val length = data?.length
                            val uploadDate = data?.uploadDate as String?
                            val md5 = data?.md5
                            val filedata = data?.data
                            if (filename != null && remoteId != null && length != null && uploadDate != null
                                && md5 != null && filedata != null
                            ) {
                                result.add(
                                    StoredFile(
                                        filename = filename,
                                        remoteId = remoteId,
                                        length = length,
                                        uploadDate = parseDate(uploadDate),
                                        md5 = md5,
                                        data = filedata
                                    )
                                )
                            } else {
                                throw IOException("Received response contained one or more null fields. Queried for: ${fileIds.joinToString()}")
                            }
                        }
                        return result
                    } else {
                        return listOf()
                    }
                } else {
                    throw IOException("Received response contained no data or user")
                }
            } else {
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    private fun parseDate(datestring: String): Date {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH)
        try {
            return dateFormat.parse(datestring) ?: throw IOException("Date parsing returned null")
        } catch (e: ParseException) {
            e.printStackTrace()
            throw e
        }
    }
}