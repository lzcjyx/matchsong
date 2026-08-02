package matchsong.app.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.EntryPointAccessors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BUG-016 回归测试：内置曲库在 App 启动时导入真实 Room 数据库。
 *
 * 经 [CatalogEntryPoint]（main 源码，测试钩子）读取应用真实数据库（与用户同路径），
 * 轮询等待启动异步导入（真实线程执行，非 Compose 测试时钟）；验证 assets 打包 +
 * 解析 + 幂等导入全链路。
 */
@RunWith(AndroidJUnit4::class)
class SongCatalogSeedTest {
    @Test
    fun startupImportPopulatesRealDatabase() {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val entryPoint =
            EntryPointAccessors.fromApplication(app, CatalogEntryPoint::class.java)
        val dao = entryPoint.songDao()

        // 启动导入为异步（Application.onCreate → appScope）；轮询至多 15s
        val count =
            kotlinx.coroutines.runBlocking {
                var current = 0
                val deadline = System.currentTimeMillis() + 15_000
                while (System.currentTimeMillis() < deadline) {
                    current = dao.count()
                    if (current > 0) break
                    kotlinx.coroutines.delay(200)
                }
                current
            }

        assertTrue("曲库应在启动后被导入（实际 $count 首）", count > 0)
        // MVP 数据集规模 52 首（zh 32 / en 20，BUG-023 扩充 2 首男调歌曲后）
        assertEquals("数据集规模应为 52 首", 52, count)
    }
}
