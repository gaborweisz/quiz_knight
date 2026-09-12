package com.trainig.quiz_knight.ui.localization

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.trainig.quiz_knight.domain.model.AppLanguage
import java.util.Locale

/**
 * The app's currently selected content language. Provided once near the Compose root
 * (see MainActivity) from SettingsRepository.observeLanguage(), independent of the
 * device's system locale.
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

/**
 * Drop-in alternative to `stringResource(...)` that resolves the string from the app's
 * own selected [LocalAppLanguage] rather than the system locale, so a toggle in-app
 * switches text immediately without recreating the Activity.
 */
@Composable
fun localizedStringResource(@StringRes id: Int, vararg formatArgs: Any): String {
    val language = LocalAppLanguage.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val localizedContext = remember(language, configuration, context) {
        val config = Configuration(configuration).apply {
            setLocale(Locale.forLanguageTag(language.code))
        }
        context.createConfigurationContext(config)
    }
    return if (formatArgs.isEmpty()) {
        localizedContext.getString(id)
    } else {
        localizedContext.getString(id, *formatArgs)
    }
}
