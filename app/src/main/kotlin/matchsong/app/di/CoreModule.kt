package matchsong.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import matchsong.app.log.AndroidLogger
import matchsong.core.common.dispatcher.DispatcherProvider
import matchsong.core.common.log.Logger
import matchsong.core.common.time.Clock
import matchsong.core.common.time.SystemClock
import javax.inject.Singleton

/**
 * 核心基础设施 DI 装配（ARCHITECTURE.md §15 CoreModule，M1.4-3）。
 *
 * 绑定：Logger → [AndroidLogger]（android.util.Log 封装，Release 自动脱敏 FR-PRIV-4）；
 * DispatcherProvider → [AndroidDispatcherProvider]（含 Dispatchers.Main）；
 * Clock → [SystemClock]。
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides
    @Singleton
    fun provideLogger(): Logger = AndroidLogger()

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = AndroidDispatcherProvider

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock
}
