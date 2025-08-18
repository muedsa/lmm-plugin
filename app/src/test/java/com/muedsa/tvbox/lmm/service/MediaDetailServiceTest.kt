package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.MediaCardType
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.lmm.TestPlugin
import com.muedsa.tvbox.lmm.checkMediaCard
import com.muedsa.tvbox.lmm.checkMediaCardRow
import com.muedsa.tvbox.tool.decodeBase64
import com.muedsa.tvbox.tool.decryptAES128CBCPKCS7
import kotlinx.coroutines.test.runTest
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.Security
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaDetailServiceTest {

    @Before
    fun init() {
        Security.addProvider(BouncyCastleProvider())
    }

    private val service = TestPlugin.provideMediaDetailService()

    @Test
    fun getDetailData_test() = runTest{
        val detail = service.getDetailData("/detail/7260.html", "/detail/7260.html")
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
            id = "/play/7118_2_1.html",
            name = "/play/7118_2_1.html",
            flag5 = "https://www.lmm97.com/play/7118_2_1.html"
        )
        val mediaPlaySource = MediaPlaySource(
            id = "test",
            name = "test",
            episodeList = listOf(mediaEpisode),
        )
        val playInfo = service.getEpisodePlayInfo(mediaPlaySource, mediaEpisode)
        check(playInfo.url.isNotEmpty())
    }

    /**
     * const key = CryptoJS.enc.Utf8.parse('ejjooopppqqqrwww');
     * const iv = CryptoJS.enc.Utf8.parse('1348987635684651');
     * const r = CryptoJS.AES.decrypt('PIAtwtNt0bsIEvWlokuhvSOjMQmsEuL+ystSSo1l5peSsJec1psjZrZYcniSqBjT', k1, {
     *     'iv': k2,
     *     'mode': CryptoJS.mode.CBC,
     *     'padding': CryptoJS.pad.Pkcs7
     * });
     * r.toString(CryptoJS.enc.Utf8) === 'ecbdf453d781cc05ac1ebba283546e2c'
     */
    @Test
    fun test() {
        val data = "PIAtwtNt0bsIEvWlokuhvSOjMQmsEuL+ystSSo1l5peSsJec1psjZrZYcniSqBjT"
            .decodeBase64()
            .decryptAES128CBCPKCS7("ejjooopppqqqrwww", "1348987635684651")
            .toString(Charsets.UTF_8)
        check("ecbdf453d781cc05ac1ebba283546e2c" == data)
    }
}