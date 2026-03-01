package com.trainig.quiz_knight.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trainig.quiz_knight.domain.model.Question
import com.trainig.quiz_knight.domain.model.QuizTopic
import com.trainig.quiz_knight.domain.repository.QuestionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Raw DTO that mirrors the JSON structure
private data class QuestionDto(
    val id: String,
    val topic: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

@Singleton
class QuestionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : QuestionRepository {

    // Simple in-memory cache so we only parse each file once per session
    private val cache = mutableMapOf<QuizTopic, List<Question>>()

    override suspend fun getQuestionsForTopic(topic: QuizTopic): List<Question> {
        cache[topic]?.let { return it }

        val fileName = "questions/${topic.name.lowercase()}.json"
        val json = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<QuestionDto>>() {}.type
        val dtos: List<QuestionDto> = gson.fromJson(json, type)

        val questions = dtos.map { dto ->
            Question(
                id = dto.id,
                topic = topic,
                text = dto.text,
                options = dto.options,
                correctIndex = dto.correctIndex
            )
        }
        cache[topic] = questions
        return questions
    }
}

