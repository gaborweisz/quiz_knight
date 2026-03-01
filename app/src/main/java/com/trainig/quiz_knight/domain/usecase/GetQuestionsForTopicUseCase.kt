package com.trainig.quiz_knight.domain.usecase

import com.trainig.quiz_knight.domain.model.Question
import com.trainig.quiz_knight.domain.model.QuizTopic
import com.trainig.quiz_knight.domain.repository.QuestionRepository
import javax.inject.Inject

/**
 * Fetches and shuffles 12 questions for a given topic.
 * If fewer than 12 exist, all are returned (shuffled).
 */
class GetQuestionsForTopicUseCase @Inject constructor(
    private val repository: QuestionRepository
) {
    companion object {
        const val QUESTIONS_PER_QUIZ = 12
    }

    suspend operator fun invoke(topic: QuizTopic): List<Question> {
        return repository.getQuestionsForTopic(topic)
            .shuffled()
            .take(QUESTIONS_PER_QUIZ)
    }
}

