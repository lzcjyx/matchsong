package matchsong.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import matchsong.data.local.consent.DataStoreConsentRepository
import matchsong.domain.port.ConsentRepository
import matchsong.domain.usecase.AcceptConsentUseCase
import matchsong.domain.usecase.GetOnboardingStatusUseCase
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 隐私说明版本常量：与 Onboarding 文案同源（SPEC §10.6）。
 * 文案变更时必须递增此版本，否则已同意用户不会重新看到新说明。
 */
const val PRIVACY_NOTICE_VERSION = "1.0"

/** ConsentRepository 实现选择 key。 */
const val KEY_REAL_CONSENT = "real"

/**
 * M2.3-2 应用级 DI 装配（main）。
 *
 * ConsentRepository 经 Map 多绑定选择：main 提供 [KEY_REAL_CONSENT]（DataStore）；
 * debug source set 的 DebugFakeBindings 提供 "fake" key（M2.4-1）。
 * 选择器按 BuildConfig.DEBUG 取 key（debug 无 fake key 时回退 real）。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @IntoMap
    @StringKey(KEY_REAL_CONSENT)
    @Singleton
    fun provideRealConsentRepository(
        @ApplicationContext context: Context,
    ): ConsentRepository = DataStoreConsentRepository(context)

    @Provides
    @Singleton
    fun provideConsentRepository(
        impls: Map<String, @JvmSuppressWildcards Provider<ConsentRepository>>,
    ): ConsentRepository {
        val key = if (matchsong.app.BuildConfig.DEBUG && impls.containsKey("fake")) "fake" else KEY_REAL_CONSENT
        return impls.getValue(key).get()
    }

    @Provides
    @Singleton
    fun provideAcceptConsentUseCase(repo: ConsentRepository): AcceptConsentUseCase = AcceptConsentUseCase(repo)

    @Provides
    @Singleton
    fun provideGetOnboardingStatusUseCase(repo: ConsentRepository): GetOnboardingStatusUseCase =
        GetOnboardingStatusUseCase(repo, PRIVACY_NOTICE_VERSION)
}
