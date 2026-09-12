package com.trainig.quiz_knight.domain.model

/**
 * Languages the app's UI and quiz content can be displayed in.
 * [code] is the BCP-47/ISO language tag used both for persistence and for
 * resolving locale-scoped string resources (see ui/localization/LocalizedStrings.kt).
 */
enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HUNGARIAN("hu", "Magyar")
}
