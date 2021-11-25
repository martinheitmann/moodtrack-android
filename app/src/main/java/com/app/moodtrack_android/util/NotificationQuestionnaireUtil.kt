package com.app.moodtrack_android.util

import android.util.Log
import com.app.moodtrack_android.model.notificationquestionnaire.NQCondition
import com.app.moodtrack_android.model.notificationquestionnaire.NQEdge
import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.query_objects.NotificationQuestionnaireByTimeOfDay
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice

class NotificationQuestionnaireUtil {
    companion object {
        val TAG = "NotifQuestionnaireUtil"
        fun getInitialQuestion(notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay): NQNode? {
            val nodes = notificationQuestionnaire.nodes
            nodes.find { n -> n.isSourceNode }?.let {
                return it
            } ?: return null
        }

        fun evaluateCondition(receivedChoice: NQQuestionChoice, condition: NQCondition) : Boolean? {
            try {
                val result: Boolean?
                val choiceValueType = receivedChoice.choiceValueType
                val choiceValue = receivedChoice.choiceValue
                val conditionString = condition.condition
                val conditionValue = condition.conditionValue
                result = when(choiceValueType){
                    NotificationChoiceType.TEXT.conditionType -> {
                        Log.d(TAG, "evaluateCondition: evaluating text condition.")
                        NotificationConditionUtil.evaluate(choiceValue, conditionString)
                    }
                    NotificationChoiceType.NUMBER.conditionType -> {
                        Log.d(TAG, "evaluateCondition: evaluating number condition.")
                        val choiceValueAsInt = choiceValue.toInt()
                        val conditionValueAsInt = conditionValue.toInt()
                        NotificationConditionUtil.evaluate(conditionString, choiceValueAsInt, conditionValueAsInt)
                    }
                    NotificationChoiceType.BOOLEAN.conditionType -> {
                        Log.d(TAG, "evaluateCondition: evaluating boolean condition.")
                        val choiceValueAsBool = choiceValue.toBoolean()
                        NotificationConditionUtil.evaluate(choiceValueAsBool, conditionString)
                    }
                    else -> return null
                }
                Log.d(TAG, "evaluateCondition: evaluating returned $result.")
                return result
            } catch(e: Throwable){
                Log.d(TAG, e.stackTraceToString())
                return null
            }
        }

        fun getOutgoingEdgesForNode(currentNode: NQNode, notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay): List<NQEdge> {
            return notificationQuestionnaire.edges.filter { e -> e.source == currentNode._id }
        }

        fun evaluateEdgeConditions(receivedChoice: NQQuestionChoice, edges: List<NQEdge>): NQEdge? {
            Log.d(TAG, "evaluateEdgeConditions: Invoked with ${edges.size} argument edges.")
            edges.forEach { edge ->
                val evalRes = evaluateCondition(receivedChoice, edge.condition)
                if(evalRes == true) return edge
            }
            return null
        }

        fun getNextNode(edgeToNextNode: NQEdge, notificationQuestionnaire: NotificationQuestionnaireByTimeOfDay): NQNode? {
            return notificationQuestionnaire.nodes.find { n -> n._id == edgeToNextNode.target }
        }
    }
}