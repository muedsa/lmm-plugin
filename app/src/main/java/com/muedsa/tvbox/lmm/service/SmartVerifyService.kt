package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.lmm.LmmHtmlParser
import com.muedsa.tvbox.lmm.model.VerifyResult
import com.muedsa.tvbox.tool.LenientJson
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.md5
import com.muedsa.tvbox.tool.post
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element

class SmartVerifyService(
    private val lmmUrlService: LmmUrlService,
    private val captchaVerifyService: CaptchaVerifyService,
    private val okHttpClient: OkHttpClient,
) {

    suspend fun verify(): Boolean {
        val ts = System.currentTimeMillis() / 1000
        val token = genToken(ts)
        val bodyStr = "${lmmUrlService.getUrl()}/index.php/ajax/smart_verify"
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
            .stringBody()
        val verifyResult = LenientJson.decodeFromString<VerifyResult>(bodyStr)
        if (verifyResult.code == 1) {
            delay(500)
            return true
        } else {
            return captchaVerifyService.tryVerify("search")
        }
    }

    fun genToken(ts: Long): String {
        return "${ts}MySecretlmm2026".md5().toHexString()
    }

    fun checkNeedValid(body: Element): Boolean = LmmHtmlParser.checkNeedValid(body)
}