package com.app.moodtrack_android.graphql

import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.CreateNotificationQuestionnaireResponseMutation
import com.app.moodtrack_android.NotificationQuestionnaireResponsesBetweenQuery
import com.app.moodtrack_android.NotificationQuestionnaireResponsesQuery
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.notificationquestionnaire.*
import com.app.moodtrack_android.model.notificationquestionnaire.inappquestionnaire.NQAppQuestionnaire
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestion
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice
import com.app.moodtrack_android.type.NQQuestionChoiceInput
import com.app.moodtrack_android.type.NotificationQuestionnaireResponseDataInput
import com.app.moodtrack_android.type.NotificationQuestionnaireResponseInput
import com.app.moodtrack_android.util.AppDateUtils
import java.io.IOException
import java.util.*
import javax.inject.Inject

/***
 * Class for receiving requests and submitting notification questionnaire responses.
 * Uses the Apollo client for GraphQL API communication.
 */
class GraphQLNotificationQuestionnaireResponseDao @Inject constructor(
    private val client: ApolloClient,
    private val tokenResolver: FirebaseAuthIdTokenResolver
) {

    val tag = "GQLNotifQuestResponseDao"

    suspend fun submitNotificationQuestionnaireResponse(
        messageId: String,
        userId: String,
        notificationQuestionnaireId: String,
        nodeId: String,
        nextNodeId: String?,
        previousNodeId: String?,
        timestamp: Date,
        responseData: NQResponseData
    ): Boolean {
        try {
            logVars(
                messageId = messageId, choiceIcon = responseData.selectedChoice.choiceIcon,
                choiceIconId = responseData.selectedChoice.choiceIconId, choiceIconMd5 = responseData.selectedChoice.choiceIconMd5,
                choiceValue = responseData.selectedChoice.choiceValue, choiceType = responseData.selectedChoice.choiceValueType
            )
            // Set all the input data for the request.
            val choices: List<NQQuestionChoiceInput> = responseData.choices.map { qc ->
                NQQuestionChoiceInput(
                    choiceValue = qc.choiceValue,
                    choiceIcon = qc.choiceIcon,
                    choiceValueType = Input.fromNullable(qc.choiceValueType),
                    choiceIconMd5 = Input.fromNullable(qc.choiceIconMd5),
                    choiceIconId = qc.choiceIconId
                )
            }
            val choice =
                NQQuestionChoiceInput(
                    choiceValue = responseData.selectedChoice.choiceValue,
                    choiceIcon = responseData.selectedChoice.choiceIcon,
                    choiceValueType = Input.fromNullable(responseData.selectedChoice.choiceValueType),
                    choiceIconMd5 = Input.fromNullable(responseData.selectedChoice.choiceIconMd5),
                    choiceIconId = responseData.selectedChoice.choiceIconId
                )

            val nqDataInput = NotificationQuestionnaireResponseDataInput(
                questionText = Input.fromNullable(responseData.questionText),
                choices = Input.fromNullable(choices),
                selectedChoice = Input.fromNullable(choice)
            )

            val notificationQuestionnaireResponseInput = NotificationQuestionnaireResponseInput(
                messageId = Input.fromNullable(messageId),
                message = Input.fromNullable(messageId),
                user = Input.fromNullable(userId),
                next = Input.fromNullable(nextNodeId),
                previous = Input.fromNullable(previousNodeId),
                timestamp = Input.fromNullable(timestamp),
                notificationQuestionnaire = Input.fromNullable(notificationQuestionnaireId),
                nodeId = Input.fromNullable(nodeId),
                node = Input.fromNullable(nodeId),
                responseData = Input.fromNullable(nqDataInput)
            )
            /*
                Since setting headers from an HTTP interceptor seems messy,
                we're instead setting the auth/id token from here.  This
                is required for authentication/authorization flows, but
                can probably be skipped in a development environment.
             */
            val idToken = tokenResolver.fetchIdToken()
            val response =
                client.mutate(
                    CreateNotificationQuestionnaireResponseMutation(
                        Input.fromNullable(
                            notificationQuestionnaireResponseInput
                        )
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
                val data = response.data
                if (data != null) {
                    return true
                } else {
                    throw IOException("Received response contained no data")
                }
            } else {
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun queryNotificationQuestionnaireResponses(userId: String): List<NQResponse> {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response =
                client.query(NotificationQuestionnaireResponsesQuery(Input.fromNullable(userId)))
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
                if (data?.notificationQuestionnaireResponses != null) {
                    val notificationQuestionnaireResponses = data.notificationQuestionnaireResponses
                    return notificationQuestionnaireResponses.map { n ->
                        NQResponse(
                            userId = n?.user?._id ?: "",
                            timestamp = n?.timestamp as Date,
                            nodeId = n.nodeId ?: "",
                            messageId = n.messageId ?: "",
                            notificationQuestionnaireId = n.notificationQuestionnaire?._id ?: "",
                            previousNodeId = "",
                            nextNodeId = "",
                            responseData = NQResponseData(
                                questionText = n.responseData?.questionText ?: "",
                                selectedChoice = NQQuestionChoice(
                                    choiceIcon = n.responseData?.selectedChoice?.choiceIcon ?: "",
                                    choiceIconId = n.responseData?.selectedChoice?.choiceIconId
                                        ?: "",
                                    choiceIconMd5 = n.responseData?.selectedChoice?.choiceIconMd5
                                        ?: "",
                                    choiceValueType = n.responseData?.selectedChoice?.choiceValueType
                                        ?: "",
                                    choiceValue = n.responseData?.selectedChoice?.choiceValue ?: "",
                                ),
                                choices = n.responseData?.choices?.map { c ->
                                    NQQuestionChoice(
                                        choiceIcon = c?.choiceIcon ?: "",
                                        choiceIconId = c?.choiceIconId ?: "",
                                        choiceIconMd5 = c?.choiceIconMd5 ?: "",
                                        choiceValueType = c?.choiceValueType ?: "",
                                        choiceValue = c?.choiceValue ?: "",
                                    )
                                } ?: listOf()
                            )
                        )
                    }
                } else {
                    throw IOException("Received response contained no data or array")
                }
            } else {
                response.errors?.forEach { error ->
                    Log.d(tag, error.message)
                }
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    suspend fun queryNotificationQuestionnaireResponsesBetween(
        userId: String,
        gte: Date,
        lte: Date
    ): List<NQResponse> {
        try {
            val idToken = tokenResolver.fetchIdToken()
            val response = client.query(
                NotificationQuestionnaireResponsesBetweenQuery(
                    Input.fromNullable(userId), Input.fromNullable(gte), Input.fromNullable(lte)
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
                val data = response.data
                if (data?.notificationQuestionnaireResponsesBetween != null) {
                    val notificationQuestionnaireResponses =
                        data.notificationQuestionnaireResponsesBetween
                    return notificationQuestionnaireResponses.map { n ->
                        NQResponse(
                            userId = n?.user?._id ?: "",
                            timestamp = AppDateUtils.parseIsoDateStringToDate(n?.timestamp as String),
                            nodeId = n.nodeId ?: "",
                            messageId = n.messageId ?: "",
                            notificationQuestionnaireId = n.notificationQuestionnaire?._id ?: "",
                            previousNodeId = "",
                            nextNodeId = "",
                            responseData = NQResponseData(
                                questionText = n.responseData?.questionText ?: "",
                                selectedChoice = NQQuestionChoice(
                                    choiceIcon = n.responseData?.selectedChoice?.choiceIcon ?: "",
                                    choiceIconId = n.responseData?.selectedChoice?.choiceIconId
                                        ?: "",
                                    choiceIconMd5 = n.responseData?.selectedChoice?.choiceIconMd5
                                        ?: "",
                                    choiceValueType = n.responseData?.selectedChoice?.choiceValueType
                                        ?: "",
                                    choiceValue = n.responseData?.selectedChoice?.choiceValue ?: "",
                                ),
                                choices = n.responseData?.choices?.map { c ->
                                    NQQuestionChoice(
                                        choiceIcon = c?.choiceIcon ?: "",
                                        choiceIconId = c?.choiceIconId ?: "",
                                        choiceIconMd5 = c?.choiceIconMd5 ?: "",
                                        choiceValueType = c?.choiceValueType ?: "",
                                        choiceValue = c?.choiceValue ?: "",
                                    )
                                } ?: listOf()
                            ),
                            node = NQNode(
                                _id = n.node?._id ?: "",
                                isSourceNode = n.node?.isSourceNode ?: false,
                                nodeLabel = n.node?.nodeLabel ?: "",
                                nqId = n.node?.nqId ?: "",
                                data = NQData(
                                    nqId = n.node?.data?.nqId ?: "",
                                    type = n.node?.data?.type ?: "",
                                    appquestionnaire = NQAppQuestionnaire(
                                        qid = n.node?.data?.appquestionnaire?.qid ?: "",
                                        customBody = n.node?.data?.appquestionnaire?.customBody ?: "",
                                        customTitle = n.node?.data?.appquestionnaire?.customTitle ?: "",
                                    ),
                                    question = NQQuestion(
                                        timeOfDay = TimeOfDay(
                                            hour = n.node?.data?.question?.timeOfDay?.hour ?: 0,
                                            minute = n.node?.data?.question?.timeOfDay?.minute ?: 0
                                        ),
                                        visible = n.node?.data?.question?.visible,
                                        questionText = n.node?.data?.question?.questionText ?: "",
                                        questionChoices = n.node?.data?.question?.questionChoices?.map { qc ->
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
                        )
                    }
                } else {
                    throw IOException("Received response contained no data or array")
                }
            } else {
                response.errors?.forEach { error ->
                    Log.d(tag, error.message)
                }
                throw IOException("Received response contained error(s): ${response.errors?.joinToString { e -> e.message }}")
            }
        } catch (e: Throwable) {
            throw e
        }
    }

    private fun logVars(
        messageId: String?,
        choiceIcon: String?,
        choiceIconId: String?,
        choiceIconMd5: String?,
        choiceValue: String?,
        choiceType: String?,
    ) {
        if (messageId == null) Log.d(tag, "MessagingBroadcastReceiver: WARNING messageId is null.")
        if (choiceIcon == null) Log.d(
            tag,
            "WARNING choiceIcon is null."
        )
        if (choiceIconId == null) Log.d(
            tag,
            "WARNING choiceIconId is null."
        )
        if (choiceIconMd5 == null) Log.d(
            tag,
            "WARNING choiceIconMd5 is null."
        )
        if (choiceValue == null) Log.d(
            tag,
            "WARNING choiceValue is null."
        )
        if (choiceType == null) Log.d(
            tag,
            "WARNING choiceType is null."
        )
    }
}