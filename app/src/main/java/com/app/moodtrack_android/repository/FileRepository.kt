package com.app.moodtrack_android.repository

import android.util.Log
import com.app.moodtrack_android.database.dao.FileDao
import com.app.moodtrack_android.graphql.GraphQLFileDao
import com.app.moodtrack_android.model.StoredFile
import javax.inject.Inject

class FileRepository @Inject constructor(
    private val fileDao: FileDao,
    private val gqlFileDao: GraphQLFileDao
) {
    val TAG = "FileRepository"

    suspend fun getFile(filename: String) : StoredFile? {
        return fileDao.findOneByName(filename)
    }

    suspend fun getFileByRemoteId(remoteId: String) : StoredFile? {
        return fileDao.findOneByRemoteId(remoteId)
    }

    suspend fun getFilesByRemoteId(remoteIds: List<String>) : List<StoredFile> {
        return fileDao.findManyByRemoteId(remoteIds.toTypedArray())
    }

    suspend fun storeFile(file: StoredFile){
        return fileDao.insert(file)
    }

    suspend fun storeMultipleFiles(files: List<StoredFile>){
        Log.d(TAG, "Inserting files with filenames: ${files.joinToString { f -> f.filename}}")
        val res = fileDao.insertAll(files)
        Log.d(TAG, "Successfully inserted files: $res")
    }

    /**
     * Checks if a StoredFile with the argument filename and remoteId exists.
     * @param filename The filename.
     * @param remoteId The remote id.
     * @return true if a StoredFile with the argument filename and remoteId exists.
     */
    suspend fun exists(filename: String, remoteId: String): Boolean {
        val res = fileDao.fileExists(filename, remoteId)
        Log.d(TAG, "call to exists() with arg $filename, $remoteId returned $res")
        return res
    }

    /**
     * Returns the list of filename-id pairs missing from the local database.
     * @param A list of filename-id pairs.
     * @return A list of filename-id pairs missing from the local database.
     */
    suspend fun multipleExists(fileNamesAndIds: List<Pair<String, String>>) : List<Pair<String, String>> {
        val filesNotExisting = mutableListOf<Pair<String, String>>()
        fileNamesAndIds.forEach { fileNameAndId ->
            if(!exists(fileNameAndId.first, fileNameAndId.second)) filesNotExisting.add(fileNameAndId)
        }
        return filesNotExisting
    }

    suspend fun fetchFiles(filenames: List<String>): List<StoredFile> {
        return gqlFileDao.queryFiles(filenames)
    }

    suspend fun fetchFilesById(fileIds: List<String>): List<StoredFile> {
        return gqlFileDao.queryFilesById(fileIds)
    }
}