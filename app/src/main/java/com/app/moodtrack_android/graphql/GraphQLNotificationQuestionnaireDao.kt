package com.app.moodtrack_android.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.NotificationQuestionnaireByTimeOfDayQuery
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.notificationquestionnaire.*
import com.app.moodtrack_android.model.notificationquestionnaire.inappquestionnaire.NQAppQuestionnaire
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestion
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice
import com.app.moodtrack_android.type.NQTODInput
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GraphQLNotificationQuestionnaireDao @Inject constructor(
    private val client: ApolloClient,
    private val tokenResolver: FirebaseAuthIdTokenResolver
) {

    suspend fun queryNotificationQuestionnaireByTimeOfDay(
        notificationQuestionnaireId: String,
        timeOfDay: TimeOfDay
    ): NotificationQuestionnaireByTimeOfDay {
        try {
            val timeOfDayInput = NQTODInput(
                minute = Input.fromNullable(timeOfDay.minute),
                hour = Input.fromNullable(timeOfDay.hour),
            )
            val notificationQuestionnaireIdInput = Input.fromNullable(notificationQuestionnaireId)
            val idToken = tokenResolver.fetchIdToken()
            val response = client.query(
                NotificationQuestionnaireByTimeOfDayQuery(
                    notificationQuestionnaireId = notificationQuestionnaireIdInput,
                    timeOfDay = Input.fromNullable(timeOfDayInput)
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
                response.data?.notificationQuestionnaireByTimeOfDay?.let { data ->
                    val nqEdges = data.edges?.map { e ->
                        NQEdge(
                            _id = e?._id ?: "",
                            nqId = e?.nqId ?: "",
                            source = e?.source ?: "",
                            target = e?.target ?: "",
                            edgeLabel = e?.edgeLabel ?: "",
                            condition = NQCondition(
                                condition = e?.condition?.condition ?: "",
                                conditionType = e?.condition?.conditionType ?: "",
                                conditionValue = e?.condition?.conditionValue ?: ""
                            )
                        )
                    } ?: emptyList()

                    val nqNodes = data.nodes?.map { n ->
                        NQNode(
                            _id = n?._id ?: "",
                            nqId = n?.nqId ?: "",
                            nodeLabel = n?.nodeLabel ?: "",
                            isSourceNode = n?.isSourceNode ?: false,
                            data = NQData(
                                nqId = n?.data?.nqId ?: "",
                                type = n?.data?.type ?: "",
                                appquestionnaire = NQAppQuestionnaire(
                                    qid = n?.data?.appquestionnaire?.qid ?: "",
                                    customBody = n?.data?.appquestionnaire?.customBody ?: "",
                                    customTitle = n?.data?.appquestionnaire?.customTitle ?: "",
                                ),
                                question = NQQuestion(
                                    timeOfDay = TimeOfDay(
                                        hour = n?.data?.question?.timeOfDay?.hour ?: 0,
                                        minute = n?.data?.question?.timeOfDay?.minute ?: 0
                                    ),
                                    questionText = n?.data?.question?.questionText ?: "",
                                    questionChoices = n?.data?.question?.questionChoices?.map { qc ->
                                        NQQuestionChoice(
                                            choiceIcon = qc?.choiceIcon ?: "",
                                            choiceValue = qc?.choiceValue ?: "",
                                            choiceValueType = qc?.choiceValueType ?: "",
                                            choiceIconId = qc?.choiceIconId ?: "",
                                            choiceIconMd5 = qc?.choiceIconMd5 ?: ""
                                        )
                                    } ?: emptyList()
                                )
                            )
                        )
                    } ?: emptyList()

                    return NotificationQuestionnaireByTimeOfDay(
                        nqId = data.nqId ?: "",
                        edges = nqEdges,
                        nodes = nqNodes
                    )

                } ?: throw IOException("Received response contained no data for the " +
                        "following params: notificationQuestionnaireId = $notificationQuestionnaireId, " +
                        "timeOfDay = $timeOfDay")
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