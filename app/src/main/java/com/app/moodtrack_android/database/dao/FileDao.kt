package com.app.moodtrack_android.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.app.moodtrack_android.model.StoredFile

@Dao
interface FileDao {
    @Query("SELECT * FROM storedfile")
    fun getAll(): List<StoredFile>

    @Query("SELECT * FROM storedfile WHERE filename IN (:filenames)")
    fun findManyByName(filenames: Array<String>): List<StoredFile>

    @Query("SELECT * FROM storedfile WHERE filename LIKE :filename  LIMIT 1")
    fun findOneByName(filename: String): StoredFile

    @Query("SELECT * FROM storedfile WHERE remoteId IN (:fileIds)")
    fun findManyByRemoteId(fileIds: Array<String>): List<StoredFile>

    @Query("SELECT * FROM storedfile WHERE remoteId LIKE :fileId  LIMIT 1")
    fun findOneByRemoteId(fileId: String): StoredFile

    @Query("SELECT EXISTS(SELECT * FROM storedfile WHERE filename LIKE :filename AND remoteId LIKE :remoteId)")
    fun fileExists(filename: String, remoteId: String) : Boolean

    @Insert
    fun insertAll(files: List<StoredFile>): List<Long>

    @Insert
    fun insert(file: StoredFile)

    @Delete
    fun delete(file: StoredFile)
}