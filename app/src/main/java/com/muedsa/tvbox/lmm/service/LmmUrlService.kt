package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.lmm.LmmConsts
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.decodeBase64ToStr
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import timber.log.Timber
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
            getUrlFromJumpPage()
        } catch (t: Throwable) {
            Timber.e(t)
            ""
        }
        if (url.isBlank()) {
            try {
                url = checkUrl( getUrlsFromGithubReop())
            } catch (_: Throwable) { }
        }
        if (url.isBlank()) {
            try {
                url = checkUrl(URLS)
            } catch (_: Throwable) { }
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
            .checkSuccess()
            .request
            .url
            .toString()

    fun getUrlsFromGithubReop(): List<String> {
        var content = ""
        for (url in GITHUB_REPO_FILE_URLS) {
            try {
                content = url.toRequestBuild()
                    .feignChrome()
                    .get(okHttpClient = okHttpClient)
                    .checkSuccess()
                    .stringBody()
            } catch (_: Throwable) {}
        }
        return if (content.isBlank()) {
            emptyList()
        } else {
            content.split("\n").map { it.decodeBase64ToStr() }
        }
    }

    companion object {
        const val JUMP_PAGE_URL = "https://www.lmmzx.com/"
        val GITHUB_REPO_FILE_URLS = listOf(
            "https://ghfast.top/https://raw.githubusercontent.com/muedsa/lmm-plugin/refs/heads/main/urls",
            "https://gh-proxy.com/raw.githubusercontent.com/muedsa/lmm-plugin/refs/heads/main/urls",
            "https://raw.githubusercontent.com/muedsa/lmm-plugin/refs/heads/main/urls",
        )
        val URLS = listOf(
            "https://lmm97.com/",
            "https://lmm28.com/",
            "https://dm.g916.com/",
        )
    }
}