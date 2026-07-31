package matchsong.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import matchsong.core.testing.fake.FakeConsentRepository
import matchsong.domain.port.ConsentRepository
import javax.inject.Singleton

/**
 * M2.4-1 debug 构建的 Fake 绑定（FR-SHELL-3）。
 *
 * 向 AppModule 的 Map 多绑定注册 "fake" key（FakeConsentRepository 内存版）。
 * debug 构建选择器优先取 fake；Release 不含本模块（无 debug source set）。
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugFakeBindings {
    @Provides
    @IntoMap
    @StringKey("fake")
    @Singleton
    fun provideFakeConsentRepository(): ConsentRepository = FakeConsentRepository()
}
