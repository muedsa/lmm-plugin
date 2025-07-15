package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.lmm.LmmConsts
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import kotlin.Throwable

class LmmUrlService(
    private val okHttpClient: OkHttpClient,
) {

    private var url: String? = null

    val mutex = Mutex()

    suspend fun getUrl(): String = mutex.withLock {
        if (url == null) {
            url = tryGetSiteUrl()
            delay(LmmConsts.DELAY)
        }
        return@withLock url ?: throw RuntimeException("获取站点地址失败")
    }

    suspend fun tryGetSiteUrl(): String {
        var url = try {
            checkUrl(getUrlFromJumpPage())
        } catch (_: Throwable) { "" }
        if (url.isBlank()) {
            url = checkUrl(getUrlsFromReleasePage())
        }
        if (url.isBlank()) throw RuntimeException("获取站点地址失败")
        return "https://${url.toHttpUrl().host}"
    }

    suspend fun checkUrl(url: String): String {
        if (url.isBlank()) throw RuntimeException("url is empty")
        url.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
        delay(LmmConsts.DELAY)
        return url
    }

    suspend fun checkUrl(urls: List<String>): String {
        var url = ""
        for (urlFromFromReleasePage in urls) {
            try {
                url = checkUrl(urlFromFromReleasePage)
                break
            } catch (_: Throwable) {}
        }
        return url
    }

    fun getUrlFromJumpPage(): String =
        JUMP_PAGE_URL.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .header("location")
            ?: throw RuntimeException("getUrlFromJumpPage failure")

    fun getUrlsFromReleasePage(): List<String> {
        return try {
            RELEASE_PAGE_URL.toRequestBuild()
                .feignChrome()
                .get(okHttpClient = okHttpClient)
                .checkSuccess()
                .parseHtml()
                .body()
                .select("#list02 ul li p a")
                .map { it.attr("href") }
                .filter { it.isBlank() }
        } catch (_: Throwable) {
            URLS
        }
    }

    companion object {
        const val RELEASE_PAGE_URL = "https://i.qg50.com/"
        const val JUMP_PAGE_URL = "https://www.lmmzx.com/"
        val URLS = listOf(
            "https://lmm97.com/",
            "https://lmm28.com/",
            "https://dm.g916.com/",
        )
    }
}