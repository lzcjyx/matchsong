package matchsong.data.local.consent

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import matchsong.domain.port.ConsentRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.consentDataStore by preferencesDataStore(name = "consent")

/**
 * M2.3-2 ConsentRepository DataStore 实现（FR-ONB-2/3）。
 *
 * 存储：accepted_version（已同意版本）、accepted_at_ms、notice_language。
 * 字段与 data-model.md §2.15 ConsentRecord 对齐（MVP 以 DataStore 承载，Room 表 M6 引入）。
 */
@Singleton
class DataStoreConsentRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ConsentRepository {
        private object Keys {
            val ACCEPTED_VERSION = stringPreferencesKey("accepted_version")
            val ACCEPTED_AT_MS = stringPreferencesKey("accepted_at_ms")
            val NOTICE_LANGUAGE = stringPreferencesKey("notice_language")
        }

        override suspend fun getAcceptedVersion(): String? =
            context.consentDataStore.data.first()[Keys.ACCEPTED_VERSION]

        override suspend fun isAccepted(version: String): Boolean =
            context.consentDataStore.data.first()[Keys.ACCEPTED_VERSION] == version

        override suspend fun accept(version: String) {
            context.consentDataStore.edit { prefs ->
                prefs[Keys.ACCEPTED_VERSION] = version
                prefs[Keys.ACCEPTED_AT_MS] = System.currentTimeMillis().toString()
                prefs[Keys.NOTICE_LANGUAGE] = "zh"
            }
        }

        override suspend fun revoke() {
            context.consentDataStore.edit { it.clear() }
        }
    }
