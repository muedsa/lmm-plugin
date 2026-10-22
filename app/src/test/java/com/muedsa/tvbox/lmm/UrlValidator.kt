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
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UrlValidator {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            ShadowLog.stream = System.out
        }
    }

    @Test
    fun valid() = runTest{
        val baseUrl = LmmUrlService.URLS.first()
        MediaDetailService.PLAYER_CONFIG_MAP.forEach { (key, value) ->
            val test = value.replaceAfter("?", "")
                .replace("{yunSite}", LmmUrlService.YUN_SITE)
            delay(200.milliseconds)
            val js = "${baseUrl}/static/player/$key.js".toRequestBuild()
                .feignChrome(referer = "$baseUrl/")
                .get(okHttpClient = TestOkHttpClient)
                .checkSuccess()
                .stringBody()
            check(js.contains(test)) { "$key check failure,\nJs:\n$js\nNot contains $test" }
        }
    }
}