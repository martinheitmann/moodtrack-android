package com.app.moodtrack_android.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.QuestionnaireQuery
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.questionnaire.*
import com.app.moodtrack_android.util.AppDateUtils
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphQLQuestionnaireDao @Inject constructor(
    private val client: ApolloClient,
    private val tokenResolver: FirebaseAuthIdTokenResolver
) {
    suspend fun queryQuestionnaire(
        questionnaireId: String
    ): QuestionnaireContent {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response = client.query(
                QuestionnaireQuery(
                    questionnaireId = Input.fromNullable(questionnaireId)
                )
            )
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
                response.data?.latestInAppQuestionnaireContent?.let { data ->
                    val id =
                        data._id ?: throw IOException("Property _id is required for questionnaire.")
                    val mQuestionnaireId = data.questionnaireId
                    val creationDate = data.creationDate as String
                    val mCreationDate = AppDateUtils.parseIsoDateStringToLocalDateTime(creationDate)
                    val freeTextItems = data.freeTextItems?.map { q ->
                        QuestionnaireFreeTextQuestion(
                            index = q?.index ?: 0,
                            question = q?.question ?: ""
                        )
                    } ?: emptyList()
                    val multiChoiceItems = data.multipleChoiceItems?.map { q ->
                        QuestionnaireMultiChoiceQuestion(
                            index = q?.index ?: 0,
                            question = q?.question ?: "",
                            choices = q?.choices?.map { c ->
                                MultiChoiceQuestionItem(
                                    display = c?.display ?: "",
                                    value = c?.value ?: "",
                                    type = c?.display ?: ""
                                )
                            } ?: emptyList()
                        )
                    } ?: emptyList()
                    return QuestionnaireContent(
                        _id = id,
                        questionnaireId = mQuestionnaireId,
                        creationDate = mCreationDate,
                        multipleChoiceItems = multiChoiceItems,
                        freeTextItems = freeTextItems
                    )

                } ?: throw IOException("Received response contained no data")
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