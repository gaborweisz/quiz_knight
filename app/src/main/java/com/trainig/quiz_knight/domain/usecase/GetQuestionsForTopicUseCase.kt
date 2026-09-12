package com.trainig.quiz_knight.domain.usecase

import com.trainig.quiz_knight.domain.model.Question
import com.trainig.quiz_knight.domain.model.QuizTopic
import com.trainig.quiz_knight.domain.repository.QuestionRepository
import com.trainig.quiz_knight.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Fetches and shuffles 12 questions for a given topic, in the app's currently
 * selected language.
 * If fewer than 12 exist, all are returned (shuffled).
 */
class GetQuestionsForTopicUseCase @Inject constructor(
    private val repository: QuestionRepository,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        const val QUESTIONS_PER_QUIZ = 12
    }

    suspend operator fun invoke(topic: QuizTopic): List<Question> {
        val language = settingsRepository.observeLanguage().first()
        return repository.getQuestionsForTopic(topic, language)
            .shuffled()
            .take(QUESTIONS_PER_QUIZ)
    }
}
