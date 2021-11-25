package com.app.moodtrack_android.misc_async

import android.util.Log
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.app.moodtrack_android.repository.FileRepository
import com.app.moodtrack_android.util.QuestionIconUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoredIconsResolver @Inject constructor(
    private val fileRepository: FileRepository,
) {
    val TAG = "StoredIconsResolver"

    /**
     * Checks if the database has all the icons contained in the argument
     * NotificationQuestionnaireByTimeOfDay object's nodes.
     */
    suspend fun checkAllIconsExistsInStorage(notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay) {
        val allChoices = QuestionIconUtil.getAllIcons(notificationQuestionnaire.nodes)
        val allUniqueChoices = allChoices.distinctBy { it.choiceIconId }
        Log.d(TAG, "Question set contained ${allChoices.size} choices")
        Log.d(TAG, "Question set contained ${allUniqueChoices.size} unqique choices")
        val filenameIdPairs = allUniqueChoices.map { choice -> Pair(choice.choiceIcon, choice.choiceIconId) }
        val iconsMissing = fileRepository.multipleExists(filenameIdPairs)
        Log.d(
            TAG,
            "Device was missing ${iconsMissing.size} icons: ${iconsMissing.joinToString()}"
        )
        if (iconsMissing.isNotEmpty()) {
            // val files = fileRepository.fetchFiles(iconsMissing)
            val fileIds = iconsMissing.map { pair -> pair.second }
            val files = fileRepository.fetchFilesById(fileIds)
            Log.d(TAG, "Fetched ${files.size} icons from remote source")
            fileRepository.storeMultipleFiles(files)
        }
    }
}