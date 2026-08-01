package matchsong.app.catalog

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import matchsong.data.local.db.dao.SongDao

/**
 * BUG-016 测试钩子：暴露真实歌曲 DAO 供仪器测试断言曲库落库
 * （SongCatalogSeedTest 经 EntryPointAccessors 读取应用真实数据库）。
 *
 * 说明：@EntryPoint 接口必须位于 main 源码——Hilt 的组件实现在 main 编译单元生成，
 * androidTest 中定义的接口无法被应用组件实现（ClassCastException）。
 * 仅测试使用，无运行时开销。
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface CatalogEntryPoint {
    fun songDao(): SongDao
}
