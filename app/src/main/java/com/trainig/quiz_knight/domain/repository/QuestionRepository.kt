package com.trainig.quiz_knight.domain.repository

import com.trainig.quiz_knight.domain.model.QuizTopic
import com.trainig.quiz_knight.domain.model.Question

/**
 * Contract for loading quiz questions.
 * Implementation reads from local JSON assets.
 */
interface QuestionRepository {
    suspend fun getQuestionsForTopic(topic: QuizTopic): List<Question>
}

