package com.trainig.quiz_knight.domain.model

/**
 * A single multiple-choice question belonging to a quiz topic.
 * Each question has exactly 4 answer options and one correct answer index (0-3).
 */
data class Question(
    val id: String,
    val topic: QuizTopic,
    val text: String,
    val options: List<String>,   // Always exactly 4 options
    val correctIndex: Int        // 0-based index into [options]
)

