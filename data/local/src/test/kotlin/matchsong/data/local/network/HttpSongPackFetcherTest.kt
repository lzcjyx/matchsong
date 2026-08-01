package matchsong.data.local.network

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * BUG-018 歌曲包下载器测试（MockWebServer，仅下载无上传）。
 */
@RunWith(RobolectricTestRunner::class)
class HttpSongPackFetcherTest {
    private lateinit var server: MockWebServer
    private val fetcher = HttpSongPackFetcher()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetch returns body for 200`() =
        runBlocking {
            server.enqueue(MockResponse().setBody("{\"ok\":true}").setHeader("Content-Type", "application/json"))

            val body = fetcher.fetch(server.url("/pack.json").toString())

            assertEquals("{\"ok\":true}", body)
        }

    @Test
    fun `fetch fails for non-2xx`() =
        runBlocking {
            server.enqueue(MockResponse().setResponseCode(404))

            try {
                fetcher.fetch(server.url("/missing.json").toString())
                fail("404 应抛异常")
            } catch (e: IOException) {
                assertTrue(e.message!!.contains("404"))
            }
        }
}
