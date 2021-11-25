package com.app.moodtrack_android.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.CreateNotificationQuestionnaireResponseMutation
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.notificationquestionnaire.NQResponseData
import com.app.moodtrack_android.type.NQQuestionChoiceInput
import com.app.moodtrack_android.type.NotificationQuestionnaireResponseDataInput
import com.app.moodtrack_android.type.NotificationQuestionnaireResponseInput
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
}