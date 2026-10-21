package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.MediaCardType
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.api.data.MediaSniffingSource
import com.muedsa.tvbox.lmm.TestOkHttpClient
import com.muedsa.tvbox.lmm.TestPlugin
import com.muedsa.tvbox.lmm.checkMediaCard
import com.muedsa.tvbox.lmm.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import java.security.Security

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaDetailServiceTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            Security.addProvider(BouncyCastleProvider())
            ShadowLog.stream = System.out
        }
    }

    private val service = TestPlugin.provideMediaDetailService()

    private val lmmUrlService = LmmUrlService(okHttpClient = TestOkHttpClient)

    @Test
    fun getDetailData_test() = runTest{
        val detail = service.getDetailData("/detail/8266.html", "/detail/8266.html")
        check(detail.id.isNotEmpty())
        check(detail.title.isNotEmpty())
        check(detail.detailUrl.isNotEmpty())
        check(detail.backgroundImageUrl.isNotEmpty())
        detail.favoritedMediaCard?.let { favoritedMediaCard ->
            checkMediaCard(favoritedMediaCard, cardType = MediaCardType.STANDARD)
            check(favoritedMediaCard.cardWidth > 0)
            check(favoritedMediaCard.cardHeight > 0)
        }
        check(detail.playSourceList.isNotEmpty())
        detail.playSourceList.forEach { mediaPlaySource ->
            check(mediaPlaySource.id.isNotEmpty())
            check(mediaPlaySource.name.isNotEmpty())
            check(mediaPlaySource.episodeList.isNotEmpty())
            mediaPlaySource.episodeList.forEach {
                check(it.id.isNotEmpty())
                check(it.name.isNotEmpty())
            }
        }
        detail.rows.forEach {
            checkMediaCardRow(it)
        }
    }

    @Test
    fun getEpisodePlayInfo_test() = runTest{
        val mediaEpisode = MediaEpisode(
            id = "/play/8266_3_1.html",
            name = "/play/8266_3_1.html",
            flag5 = "${lmmUrlService.getUrl()}/play/8266_3_1.html"
        )
        val mediaPlaySource = MediaPlaySource(
            id = "test",
            name = "test",
            episodeList = listOf(mediaEpisode),
        )
        val playInfo = service.getEpisodePlayInfo(mediaPlaySource, mediaEpisode)
        check(playInfo !is MediaSniffingSource)
        check(playInfo.url.isNotEmpty())
        println(playInfo.url)
    }
}