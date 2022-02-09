package com.app.moodtrack_android.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.app.moodtrack_android.R
import com.app.moodtrack_android.database.dao.FileDao
import com.app.moodtrack_android.graphql.GraphQLFileDao
import com.app.moodtrack_android.model.StoredFile
import org.apache.commons.io.FilenameUtils
import java.io.IOException
import javax.inject.Inject

class FileRepository @Inject constructor(
    private val fileDao: FileDao,
    private val gqlFileDao: GraphQLFileDao
) {
    val tag = "FileRepository"

    suspend fun getFile(filename: String): StoredFile? {
        return fileDao.findOneByName(filename)
    }

    suspend fun getFileByRemoteId(remoteId: String): StoredFile? {
        return fileDao.findOneByRemoteId(remoteId)
    }

    suspend fun getFilesByRemoteId(remoteIds: List<String>): List<StoredFile> {
        return fileDao.findManyByRemoteId(remoteIds.toTypedArray())
    }

    suspend fun storeFile(file: StoredFile) {
        return fileDao.insert(file)
    }

    suspend fun storeMultipleFiles(files: List<StoredFile>) {
        Log.d(tag, "Inserting files with filenames: ${files.joinToString { f -> f.filename }}")
        val res = fileDao.insertAll(files)
        Log.d(tag, "Successfully inserted files: $res")
    }

    /**
     * Checks if a StoredFile with the argument filename and remoteId exists.
     * @param filename The filename.
     * @param remoteId The remote id.
     * @return true if a StoredFile with the argument filename and remoteId exists.
     */
    suspend fun exists(filename: String, remoteId: String): Boolean {
        val res = fileDao.fileExists(filename, remoteId)
        Log.d(tag, "call to exists() with arg $filename, $remoteId returned $res")
        return res
    }

    /**
     * Returns the list of filename-id pairs missing from the local database.
     * @param A list of filename-id pairs.
     * @return A list of filename-id pairs missing from the local database.
     */
    suspend fun multipleExists(fileNamesAndIds: List<Pair<String, String>>): List<Pair<String, String>> {
        val filesNotExisting = mutableListOf<Pair<String, String>>()
        fileNamesAndIds.forEach { fileNameAndId ->
            if (!exists(fileNameAndId.first, fileNameAndId.second)) filesNotExisting.add(
                fileNameAndId
            )
        }
        return filesNotExisting
    }

    suspend fun fetchFiles(filenames: List<String>): List<StoredFile> {
        return gqlFileDao.queryFiles(filenames)
    }

    suspend fun fetchFilesById(fileIds: List<String>): List<StoredFile> {
        return gqlFileDao.queryFilesById(fileIds)
    }

    suspend fun getIconFilesRequestAndSaveMissing(fileIds: List<String>): List<StoredFile> {
        if (fileIds.isEmpty()) {
            Log.d(tag, "getIconFilesRequestAndSaveMissing: fileIds arg is empty.")
            return emptyList()
        }
        val files = mutableListOf<StoredFile>()
        val missing = mutableListOf<String>()
        val all = mutableListOf<StoredFile>()
        for (id in fileIds) {
            val file = getFileByRemoteId(id)
            if (file != null) files.add(file) else missing.add(id)
        }
        Log.d(tag, "getIconFilesRequestAndSaveMissing: found ${files.size} local files.")
        all.addAll(files)
        if (missing.isNotEmpty()) {
            val missingIcons = gqlFileDao.queryFilesById(missing)
            if (missingIcons.isNotEmpty()) {
                all.addAll(missingIcons)
                storeMultipleFiles(missingIcons)
            }
            Log.d(
                tag,
                "getIconFilesRequestAndSaveMissing: found ${missingIcons.size} remote files."
            )
        }
        Log.d(tag, "getIconFilesRequestAndSaveMissing: returned ${all.size} total files.")
        return all
    }

    /***
     * Converts a file to a bitmap. Accepts .png and .jpg file
     * extensions. File must be base64 encoded.
     */
    fun convertStoredFileToBitmap(storedFile: StoredFile): Bitmap {
        try {
            val filename = storedFile.filename
            val bitmap: Bitmap?
            val ext = FilenameUtils.getExtension(filename) // returns file extension
            // Check since only png, jpg and bmp is supported
            if (ext == "png" || ext == "jpg") {
                val data: String = storedFile.data
                val decodedString: ByteArray = Base64.decode(data, Base64.DEFAULT)
                val bitmapFromString =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                bitmap = bitmapFromString
                return bitmap
                    ?: throw IOException("Could not convert bitmap for file with name $filename.")
            } else {
                throw IOException("Invalid file format received. Found $ext.")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    fun convertBase64StringToBitmap(filename: String, data: String): Bitmap? {
        val ext = FilenameUtils.getExtension(filename)
        if (ext == "png" || ext == "jpg") {
            val decodedString: ByteArray = Base64.decode(data, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } else Log.d(tag, "convertBase64StringToBitmap: invalid file format.")
        return null
    }

    fun getUnknownOrPlaceholderIcon(context: Context): Bitmap {
        //return BitmapFactory.decodeResource(context.resources, R.drawable.ic_baseline_help_24)
        return AppCompatResources.getDrawable(context, R.drawable.ic_baseline_help_24)?.toBitmap()
            ?: throw IOException("Could not create bitmap from resource.")
    }

    suspend fun getIconFileRequestAndSaveIfMissing(remoteId: String): StoredFile? {
        if (remoteId.isNotBlank()) {
            val file = getFileByRemoteId(remoteId)
            if (file != null) return file
            val fetchedFile = gqlFileDao.queryFilesById(listOf(remoteId))
            if (fetchedFile.size == 1) return fetchedFile.first()
        } else Log.d(tag, "getIconFileRequestAndSaveIfMissing: remoteId was blank.")
        return null
    }
}