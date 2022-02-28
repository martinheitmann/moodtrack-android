package com.app.moodtrack_android.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.FileUpload
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.*
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.DocumentFile
import com.app.moodtrack_android.model.StoredFile
import okio.BufferedSink
import java.io.File
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphQLDocumentDao @Inject constructor(
    private val client: ApolloClient,
    private val tokenResolver: FirebaseAuthIdTokenResolver
) {

    suspend fun uploadDocument(filename: String, mimetype: String, file: File, userId: String) {
        val idToken = tokenResolver.fetchIdToken()
        client.mutate(CreateDocumentMutation(
            ownerId = userId,
            file = object : FileUpload(mimetype) {
                override fun contentLength(): Long {
                    return file.length()
                }

                override fun fileName(): String {
                    return filename
                }

                override fun writeTo(sink: BufferedSink) {
                    val barr = file.readBytes()
                    sink.write(barr)
                }
            }
        )).toBuilder()
            .requestHeaders(
                RequestHeaders
                    .builder()
                    .addHeader("Authorization", idToken)
                    .build()
            )
            .build()
            .await()

    }

    suspend fun queryDocumentList(userId: String): List<DocumentFile> {
        val idToken = tokenResolver.fetchIdToken()
        val response = client.query(DocumentsListQuery(userId))
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
            if (data?.documentsByOwner != null) {
                val documentData = data.documentsByOwner
                if (documentData.isNotEmpty()) {
                    val result = mutableListOf<DocumentFile>()
                    documentData.forEach { queryData ->
                        val filename = queryData?.filename
                        val length = queryData?.length
                        val uploadDate = queryData?.uploadDate as String?
                        val md5 = queryData?.md5
                        val ownerId = queryData?.ownerId
                        val id = queryData?._id
                        if (filename != null && length != null && uploadDate != null
                            && md5 != null && ownerId != null && id != null
                        ) {
                            result.add(
                                DocumentFile(
                                    id = id,
                                    filename = filename,
                                    length = length,
                                    uploadDate = parseDate(uploadDate),
                                    md5 = md5,
                                    ownerId = ownerId,
                                    data = null
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
                throw IOException("Received response contained no documentsByOwner field")
            }
        } else {
            throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
        }
    }

    suspend fun queryDocument(userId: String, id: String): DocumentFile {
        val idToken = tokenResolver.fetchIdToken()
        val response =
            client.query(DocumentQuery(userId, id))
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
            if (data?.documentByOwner != null) {
                val documentData = data.documentByOwner
                val docId = documentData._id
                val filename = documentData.filename
                val length = documentData.length
                val uploadDate = documentData.uploadDate as String?
                val md5 = documentData.md5
                val ownerId = documentData.ownerId
                val fileData = documentData.data
                if (length != null && uploadDate != null
                    && md5 != null
                ) {
                    return DocumentFile(
                        id = docId,
                        filename = filename,
                        length = length,
                        uploadDate = parseDate(uploadDate),
                        md5 = md5,
                        ownerId = ownerId,
                        data = fileData
                    )
                } else {
                    throw IOException("Received response contained one or more null fields")
                }
            } else {
                throw IOException("Received response contained no 'documentByOwner' field")
            }
        } else {
            throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
        }
    }

    suspend fun deleteDocument(id: String, userId: String) {
        val idToken = tokenResolver.fetchIdToken()
        client.mutate(DeleteDocumentMutation(_id = id, ownerId = userId))
            .toBuilder()
            .requestHeaders(
                RequestHeaders
                    .builder()
                    .addHeader("Authorization", idToken)
                    .build()
            )
            .build()
            .await()
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