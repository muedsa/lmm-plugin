package com.muedsa.tvbox.lmm

import com.muedsa.tvbox.lmm.service.LmmUrlService
import com.muedsa.tvbox.lmm.service.MediaDetailService
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UrlValidator {

    private val service = LmmUrlService(okHttpClient = TestOkHttpClient)

    @Test
    fun valid() = runTest{
        val baseUrl = service.getUrl()
        MediaDetailService.PLAYER_CONFIG_MAP.forEach { (key, value) ->
            val test = value.replaceAfter("?", "")
            delay(200)
            val js = "${baseUrl}/static/player/$key.js".toRequestBuild()
                .feignChrome(referer = "$baseUrl/")
                .get(okHttpClient = TestOkHttpClient)
                .checkSuccess()
                .stringBody()
            check(js.contains(test)) { "$key check failure,\nJs:\n$js\nNot contains $test" }
        }
    }
}