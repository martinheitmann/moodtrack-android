package com.app.moodtrack_android.ui.questionnaire

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.app.moodtrack_android.model.questionnaire.*
import com.app.moodtrack_android.model.questionnaireresponse.QuestionnaireResponse
import com.app.moodtrack_android.model.questionnaireresponse.QuestionnaireResponseFreeTextItem
import com.app.moodtrack_android.model.questionnaireresponse.QuestionnaireResponseMultiChoiceAlternative
import com.app.moodtrack_android.model.questionnaireresponse.QuestionnaireResponseMultiChoiceItem
import com.app.moodtrack_android.repository.QuestionnaireRepository
import com.app.moodtrack_android.repository.QuestionnaireResponseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class QuestionnaireViewModel @Inject constructor(
    application: Application,
    private val questionnaireRepository: QuestionnaireRepository,
    private val questionnaireResponseRepository: QuestionnaireResponseRepository
) : AndroidViewModel(application) {

    val TAG = "QuestionnaireViewModel"

    var currentQuestion: MutableLiveData<QuestionnaireElement> = MutableLiveData()
    var registeredChoices = mutableMapOf<Int, Any>()
    var questionnaire: QuestionnaireContent? = null
    var questionnaireList: List<QuestionnaireElement> = emptyList()
    val precedingNotificationQuestion: String? = null
    var isDryRun: Boolean? = null
    var messageId: String? = null

    var currentTextInput: String = ""
    var currentChoiceIndex: Int = -1

    var isSubmitting = MutableLiveData(false)

    /**
     * Returns true if the current index is the last.
     */
    fun isFinalQuestion(index: Int, listSize: Int): Boolean{
        return index == listSize - 1
    }

    /**
     * Fetches the questionnaire with the argument id
     * from the repository and converts it to a list format.
     * @param id of the questionnaire to fetch.
     */
    fun getQuestionnaire(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            questionnaireRepository.getQuestionnaire(id)?.let {
                questionnaire = it
                questionnaireList = convertQuestionnaireToList(it)
                currentQuestion.postValue(questionnaireList.first())
            }
        }
    }

    /**
     * Converts a questionnaire to a list representation.
     * @param questionnaire the questionnarire to convert
     * @return A list of QuestionnaireElement representing the questionnaire
     */
    private fun convertQuestionnaireToList(questionnaire: QuestionnaireContent): List<QuestionnaireElement> {
        val l1 = questionnaire.freeTextItems
        val l2 = questionnaire.multipleChoiceItems
        return (l1 + l2).sortedBy { e -> e.index }
    }

    /**
     * Fetches and sets the next question in th list.
     */
    fun getNextQuestion() {
        val mCurrentQuestion = currentQuestion.value
        if (mCurrentQuestion is QuestionnaireMultiChoiceQuestion) {
            if (currentChoiceIndex > -1) {
                registeredChoices[mCurrentQuestion.index] = currentChoiceIndex
                currentChoiceIndex = -1
            }
        }
        if (mCurrentQuestion is QuestionnaireFreeTextQuestion) {
            if (currentTextInput.isNotEmpty())
                registeredChoices[mCurrentQuestion.index] = currentTextInput
            Log.d(TAG, "Stored text $currentTextInput for index ${mCurrentQuestion.index}")
        }
        if (mCurrentQuestion != null) {
            if (mCurrentQuestion.index < questionnaireList.size - 1) {
                val nextQuestion = questionnaireList[mCurrentQuestion.index + 1]
                currentQuestion.postValue(nextQuestion)
            }
        }
    }

    /**
     * Fetches and sets the previous question in the list.
     */
    fun getPreviousQuestion() {
        val mCurrentQuestion = currentQuestion.value
        if (mCurrentQuestion is QuestionnaireMultiChoiceQuestion) {
            if (currentChoiceIndex > -1) {
                registeredChoices[mCurrentQuestion.index] = currentChoiceIndex
                currentChoiceIndex = -1
            }
        }
        if (mCurrentQuestion is QuestionnaireFreeTextQuestion) {
            if (currentTextInput.isNotEmpty())
                registeredChoices[mCurrentQuestion.index] = currentTextInput
        }
        if (mCurrentQuestion != null) {
            if (mCurrentQuestion.index != 0) {
                val nextQuestion = questionnaireList[mCurrentQuestion.index - 1]
                currentQuestion.postValue(nextQuestion)
            }
        }
    }

    // Sets the current input text.
    fun setNewCurrentInputText(s: String) {
        currentTextInput = s
    }

    // Not in use
    fun clearCurrentInputText() {
        currentTextInput = ""
    }

    /**
     * Collects and creates the questionnaire response data to be
     * sent by the client.
     */
    private fun createQuestionnaireResponseData(): QuestionnaireResponse? {
        // Find the current user to bind the userId of the response to.
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val questionnaireId = questionnaire?.questionnaireId
        val questionnaireContentId = questionnaire?._id
        val precedingQuestion = precedingNotificationQuestion ?: ""

        // User ID and questionnaire ID should not be null, so print warning to logger.
        if (userId == null) Log.d(
            TAG,
            "WARNING (createQuestionnaireResponseData): userId was null."
        )
        if (questionnaireId == null) Log.d(
            TAG,
            "WARNING (createQuestionnaireResponseData): questionnaireId was null."
        )

        if (questionnaireContentId == null) Log.d(
            TAG,
            "WARNING (createQuestionnaireResponseData): questionnaireContentId was null."
        )

        if (userId != null && questionnaireId != null && questionnaireContentId != null) {
            val timestamp = Date()
            return QuestionnaireResponse(
                timestamp = timestamp,
                user = userId,
                questionnaire = questionnaireId,
                questionnaireContent = questionnaireContentId,
                precedingNotificationQuestion = precedingQuestion,
                multipleChoiceItems = createMultiChoiceResponses(),
                freeTextItems = createFreeTextResponses()
            )
        } else Log.d(TAG, "userId, questionnaireId or questionnaireName was null, returning null.")
        return null
    }

    /**
     * Creates a multi-choice questionnaire response from
     * a multi-choice question item.
     */
    private fun createMultiChoiceResponses(): List<QuestionnaireResponseMultiChoiceItem> {
        return questionnaireList
            .filterIsInstance<QuestionnaireMultiChoiceQuestion>()
            .map { item ->
                val selectedChoice = item.choices[registeredChoices[item.index] as Int]
                QuestionnaireResponseMultiChoiceItem(
                    index = item.index,
                    question = item.question,
                    selectedChoice = QuestionnaireResponseMultiChoiceAlternative(
                        display = selectedChoice.display,
                        value = selectedChoice.value,
                        type = selectedChoice.type
                    ),
                    choices = item.choices.map { c ->
                        QuestionnaireResponseMultiChoiceAlternative(
                            display = c.display,
                            value = c.value,
                            type = c.type
                        )
                    }
                )
            }
    }

    /**
     * Creates a free-text questionnaire response from
     * a free-text question item.
     */
    private fun createFreeTextResponses(): List<QuestionnaireResponseFreeTextItem> {
        return questionnaireList
            .filterIsInstance<QuestionnaireFreeTextQuestion>()
            .map { item ->
                QuestionnaireResponseFreeTextItem(
                    index = item.index,
                    question = item.question,
                    response = registeredChoices[item.index] as String
                )
            }
    }

    /**
     * Stores the response to the currently
     * selected question.
     */
    private fun setCurrentQuestionResponse() {
        currentQuestion.value?.let { q ->
            if (q is QuestionnaireFreeTextQuestion) {
                registeredChoices[q.index] = currentTextInput
            }
            if (q is QuestionnaireMultiChoiceQuestion) {
                registeredChoices[q.index] = currentChoiceIndex
            }
        }
    }

    /**
     * Submits the questionnaire response. Calls the argument function
     * if the operation is successful.
     * @param onSuccess the function to call upon success.
     */
    fun submitQuestionnaireResponse(onSuccess: () -> Unit) {
        setCurrentQuestionResponse()
        if (areAllQuestionsAnswered()) {
            val questionnaireResponse = createQuestionnaireResponseData()
            if (questionnaireResponse != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val mIsDryRun = isDryRun
                        isSubmitting.postValue(true)
                        if(mIsDryRun != null && !mIsDryRun){
                            questionnaireResponseRepository.submitQuestionnaireResponse(
                                questionnaireResponse,
                                messageId
                            )
                        } else {
                            Log.d(TAG, "Dry run registered, skipping submission.")
                        }
                    } catch (e: Throwable) {
                        Log.d(TAG, e.stackTraceToString())
                    } finally {
                        isSubmitting.postValue(false)
                        onSuccess()
                    }
                }
            } else Log.d(TAG, "questionnaireResponse was null, skipping submission")
        } else {
            val text = "Alle spørsmål må besvares før innsending"
            val duration = Toast.LENGTH_SHORT

            val toast = Toast.makeText(getApplication(), text, duration)
            toast.show()
        }
    }

    /**
     * Checks if a question is answered.
     * @param question the question to check.
     * @return true if the question is answered with a value,
     * false otherwise.
     */
    private fun isQuestionAnswered(question: QuestionnaireElement): Boolean {
        val choice = registeredChoices[question.index]
        if((choice as? Int) == -1) return false // This means it probably hasn't been set.
        if (choice != null) {
            if (question is QuestionnaireFreeTextQuestion) {
                val questionResponse = choice as String
                if (questionResponse.isEmpty()) return false
                return true
            }
            return true
        }
        return false
    }

    /**
     * Checks if all questions are answered.
     * @return true if all questions are answered with a value,
     * false otherwise.
     */
    private fun areAllQuestionsAnswered(): Boolean {
        questionnaireList.forEach {
            if (!isQuestionAnswered(it)) return false
        }
        return true
    }
}