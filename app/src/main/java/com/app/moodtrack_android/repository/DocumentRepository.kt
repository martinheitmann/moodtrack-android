package com.app.moodtrack_android.repository

import android.util.Log
import com.app.moodtrack_android.graphql.GraphQLDocumentDao
import com.app.moodtrack_android.model.DocumentFile
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val authRepository: AuthRepository,
    private val docRepository: GraphQLDocumentDao
) {
    val tag = "DocumentRepository"

    suspend fun uploadDocument(filename: String, file: File, mimetype: String) {
        val userId = authRepository.getUid()
        if (userId != null) {
            docRepository.uploadDocument(
                file = file,
                filename = filename,
                mimetype = mimetype,
                userId = userId
            )
        }
    }

    suspend fun getDocumentList(): List<DocumentFile> {
        val userId = authRepository.getUid()
        if (userId != null) {
            return docRepository.queryDocumentList(userId)
        }
        Log.d(tag, "a userId of null was received while attempting to fetch document list.")
        return emptyList()
    }

    suspend fun getDocument(id: String): DocumentFile? {
        try {
            val userId = authRepository.getUid()
            if (userId != null) {
                return docRepository.queryDocument(userId, id)
            }
            Log.d(tag, "a userId of null was received while attempting to fetch document.")
            return null
        } catch(exception: Throwable){
            Log.d(tag, exception.stackTraceToString())
            return null
        }
    }

    suspend fun deleteDocument(id: String){
        val userId = authRepository.getUid()
        if (userId != null) {
            docRepository.deleteDocument(id = id, userId = userId)
        }
    }
}