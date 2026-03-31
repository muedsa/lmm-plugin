package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.lmm.LmmHtmlParser
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.md5
import com.muedsa.tvbox.tool.post
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.FormBody
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element

class SmallVerifyService(
    private val lmmUrlService: LmmUrlService,
    private val okHttpClient: OkHttpClient,
) {

    suspend fun verify() {
        val ts = System.currentTimeMillis() / 1000
        val token = genToken(ts)
        "${lmmUrlService.getUrl()}/index.php/ajax/smart_verify"
            .toRequestBuild()
            .feignChrome()
            .post(
                body = FormBody.Builder()
                    .add("smart_token", token)
                    .add("ts", "$ts")
                    .build(),
                okHttpClient = okHttpClient
            )
            .checkSuccess()
    }

    fun genToken(ts: Long): String {
        return "${ts}MySecretlmm2026".md5().toHexString()
    }

    fun checkNeedValid(body: Element): Boolean = LmmHtmlParser.checkNeedValid(body)
}