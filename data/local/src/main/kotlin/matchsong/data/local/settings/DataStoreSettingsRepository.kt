package matchsong.data.local.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import matchsong.domain.port.SettingsRepository
import matchsong.domain.port.UserSettings
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * M7.6-2 UserSettings DataStore 实现（语言/偏好/排除风格）。
 */
@Singleton
class DataStoreSettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SettingsRepository {
        private object Keys {
            val LANGUAGE = stringPreferencesKey("language")
            val PREFERRED_GENRES = stringSetPreferencesKey("preferred_genres")
            val EXCLUDED_GENRES = stringSetPreferencesKey("excluded_genres")
            val ONBOARDING_DONE = stringPreferencesKey("onboarding_done")
        }

        override suspend fun getSettings(): UserSettings {
            val prefs = context.settingsDataStore.data.first()
            return UserSettings(
                language = prefs[Keys.LANGUAGE] ?: "zh",
                preferredGenres = prefs[Keys.PREFERRED_GENRES]?.toList() ?: emptyList(),
                excludedGenres = prefs[Keys.EXCLUDED_GENRES]?.toList() ?: emptyList(),
            )
        }

        override suspend fun saveSettings(settings: UserSettings) {
            context.settingsDataStore.edit { prefs ->
                prefs[Keys.LANGUAGE] = settings.language
                prefs[Keys.PREFERRED_GENRES] = settings.preferredGenres.toSet()
                prefs[Keys.EXCLUDED_GENRES] = settings.excludedGenres.toSet()
            }
        }

        override suspend fun isOnboardingCompleted(): Boolean =
            context.settingsDataStore.data.first()[Keys.ONBOARDING_DONE] == "true"

        override suspend fun setOnboardingCompleted(completed: Boolean) {
            context.settingsDataStore.edit { prefs ->
                prefs[Keys.ONBOARDING_DONE] = completed.toString()
            }
        }

        /** M9.3 清空全部设置与 Onboarding 标记（删除全部数据，FR-HX-4/ACC-15）。 */
        override suspend fun clear() {
            context.settingsDataStore.edit { prefs -> prefs.clear() }
        }
    }
