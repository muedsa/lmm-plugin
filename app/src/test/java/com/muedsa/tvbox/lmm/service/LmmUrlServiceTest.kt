package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.lmm.TestOkHttpClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LmmUrlServiceTest {

    private val service = LmmUrlService(okHttpClient = TestOkHttpClient)

    @Test
    fun url_test() = runTest{
        val url = service.getUrl()
        println(url)
        check(url.isNotBlank())
    }

    @Test
    fun getUrlFromJumpPage_test() = runTest{
        val url = service.getUrlFromJumpPage()
        println(url)
        check(url.isNotBlank())
    }

    @Test
    fun getUrlsFromGithubReop_test() = runTest{
        val urls = service.getUrlsFromGithubReop()
        urls.forEach { println(it) }
        check(urls.isNotEmpty())
    }
}