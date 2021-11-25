package com.app.moodtrack_android.graphql

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.request.RequestHeaders
import com.app.moodtrack_android.CreateQuestionnaireResponseMutation
import com.app.moodtrack_android.auth.FirebaseAuthIdTokenResolver
import com.app.moodtrack_android.model.questionnaireresponse.QuestionnaireResponse
import com.app.moodtrack_android.type.InAppQuestionnaireFreeTextResponseInput
import com.app.moodtrack_android.type.InAppQuestionnaireMultipleChoiceItemInput
import com.app.moodtrack_android.type.InAppQuestionnaireMultipleChoiceResponseInput
import com.app.moodtrack_android.type.InAppQuestionnaireResponseInput
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/***
 * Class for receiving requests and submitting questionnaire responses.
 * Uses the Apollo client for GraphQL API communication.
 */
@Singleton
class GraphQLQuestionnaireResponseDao @Inject constructor(
    private val client: ApolloClient,
    private val tokenResolver: FirebaseAuthIdTokenResolver
) {

    suspend fun submitQuestionnaireResponse(
        questionnaireResponse: QuestionnaireResponse,
        messageId: String?
    ): Boolean {
        try {
            // Set all the input data for the request.
            val timestamp = questionnaireResponse.timestamp
            val user = questionnaireResponse.user
            val questionnaire = questionnaireResponse.questionnaire
            val questionnaireContent = questionnaireResponse.questionnaireContent
            val fqin: List<InAppQuestionnaireFreeTextResponseInput> =
                questionnaireResponse.freeTextItems.map { fcin ->
                    InAppQuestionnaireFreeTextResponseInput(
                        index = fcin.index,
                        question = fcin.question,
                        response = fcin.response
                    )
                }
            val mcin: List<InAppQuestionnaireMultipleChoiceResponseInput> =
                questionnaireResponse.multipleChoiceItems.map { mci ->
                    InAppQuestionnaireMultipleChoiceResponseInput(
                        index = mci.index,
                        question = mci.question,
                        selectedChoice = InAppQuestionnaireMultipleChoiceItemInput(
                            display = Input.fromNullable(mci.selectedChoice.display),
                            value = mci.selectedChoice.value,
                            type = Input.fromNullable(mci.selectedChoice.type)
                        ),
                        choices = Input.fromNullable(mci.choices.map { c ->
                            InAppQuestionnaireMultipleChoiceItemInput(
                                display = Input.fromNullable(c.display),
                                value = c.value,
                                type = Input.fromNullable(c.type)
                            )
                        })
                    )
                }

            val questionnaireResponseInput = InAppQuestionnaireResponseInput(
                timestamp = Input.fromNullable(timestamp),
                message = Input.fromNullable(messageId),
                messageId = Input.fromNullable(messageId),
                name = "",
                user = user,
                questionnaire = questionnaire,
                questionnaireContent = questionnaireContent,
                multipleChoiceItems = mcin,
                freeTextItems = fqin
            )
            /*
                Since setting headers from an HTTP interceptor seems messy,
                we're instead setting the auth/id token from here.  This
                is required for authentication/authorization flows, but
                can probably be skipped in a development environment.
             */
            val idToken = tokenResolver.fetchIdToken()
            val response = client.mutate(
                CreateQuestionnaireResponseMutation(
                    Input.fromNullable(questionnaireResponseInput)
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
                return true
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