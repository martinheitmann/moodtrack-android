package com.app.moodtrack_android.util

import com.app.moodtrack_android.model.notificationquestionnaire.NQNode
import com.app.moodtrack_android.model.notificationquestionnaire.question.NQQuestionChoice

class QuestionIconUtil {
    companion object {
        private fun <T> concatenate(vararg lists: List<T>): List<T> {
            return listOf(*lists).flatten()
        }

        private fun getQuestionIconNames(nodes: List<NQNode>): MutableList<String> {
            val iconFileNames = mutableListOf<String>()
            nodes.forEach { node ->
                if(node.data.type == "question"){
                    val questionChoices = node.data.question.questionChoices
                    questionChoices.forEach { choice ->
                        val iconFilename = choice.choiceIcon
                        iconFileNames.add(iconFilename)
                    }
                }
            }
            return iconFileNames
        }

        private fun getQuestionIcons(nodes: List<NQNode>): MutableList<NQQuestionChoice> {
            val icons = mutableListOf<NQQuestionChoice>()
            nodes.forEach { node ->
                if(node.data.type == "question"){
                    val questionChoices = node.data.question.questionChoices
                    icons.addAll(questionChoices)
                }
            }
            return icons
        }

        fun getAllIconNames(notificationQuestions: List<NQNode>): List<String> {
            val questionIcons = getQuestionIconNames(notificationQuestions)
            return concatenate(questionIcons)
        }

        fun getAllIcons(notificationQuestions: List<NQNode>): List<NQQuestionChoice> {
            val questionIcons = getQuestionIcons(notificationQuestions)
            return concatenate(questionIcons)
        }
    }
}