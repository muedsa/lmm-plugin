package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.DanmakuData
import com.muedsa.tvbox.api.data.DanmakuDataFlow
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.data.MediaDetail
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaHttpSource
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.api.data.MediaSniffingSource
import com.muedsa.tvbox.api.data.SavedMediaCard
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.lmm.LmmConsts
import com.muedsa.tvbox.lmm.LmmHtmlParser
import com.muedsa.tvbox.lmm.model.PlayerAAAA
import com.muedsa.tvbox.lmm.model.VxdevResp
import com.muedsa.tvbox.tool.LenientJson
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.decodeBase64
import com.muedsa.tvbox.tool.decodeBase64ToStr
import com.muedsa.tvbox.tool.decryptAES128CBCPKCS7
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.delay
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element

class MediaDetailService(
    private val lmmUrlService: LmmUrlService,
    private val okHttpClient: OkHttpClient,
) : IMediaDetailService {

    override suspend fun getDetailData(mediaId: String, detailUrl: String): MediaDetail {
        if (!detailUrl.startsWith("/detail")) throw RuntimeException("不支持的地址")
        val url = "${lmmUrlService.getUrl()}$detailUrl"
        val body = url.toRequestBuild()
            .feignChrome(referer = "${lmmUrlService.getUrl()}/")
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        LmmHtmlParser.checkMacMsg(body)
        val infoBoxEl = body.selectFirst("#site-content .detail section .infobox")
            ?: throw RuntimeException("获取视频详情失败")
        val coverImgUrl = infoBoxEl.selectFirst(".module-item-pic img")
            ?.attr("src")
            ?: ""
        val title = (infoBoxEl.selectFirst(".video-info .video-info-header .page-title")?.text()
            ?.trim()
            ?: throw RuntimeException("获取视频标题失败"))
        val playListEl = body.selectFirst("#site-content .detail section.video-info .playlist")
            ?: throw RuntimeException("获取播放列表失败")
        val playerTabs = playListEl
            .select(".module-heading .module-tab.module-player-tab .module-tab-items .module-tab-content .module-tab-item.tab-item")
            .map { it.text().trim() }
        return MediaDetail(
            id = mediaId,
            title = title,
            subTitle = buildList {
                infoBoxEl.selectFirst(".video-info .video-info-header .video-subtitle")
                    ?.text()
                    ?.trim()
                    ?.let { add(it) }
                infoBoxEl.select(".video-info .video-info-header .video-info-aux a.tag-link")
                    .map { it.text().trim() }
                    .let { addAll(it) }
            }.joinToString(" | "),
            description = infoBoxEl.select(".video-info .video-info-main .video-info-items")
                .joinToString("\n") { it.text().trim() },
            detailUrl = detailUrl,
            backgroundImageUrl = coverImgUrl,
            playSourceList = playListEl.select(".module-list.module-player-list")
                .mapIndexed { index, mEl ->
                    val episodes = mEl.select(".module-blocklist a").map { aEl ->
                        MediaEpisode(
                            id = aEl.attr("href"),
                            name = aEl.text().trim(),
                            flag5 = url,
                        )
                    }
                    MediaPlaySource(
                        id = playerTabs[index],
                        name = playerTabs[index],
                        episodeList = episodes,
                    )
                },
            favoritedMediaCard = SavedMediaCard(
                id = mediaId,
                title = title,
                detailUrl = detailUrl,
                coverImageUrl = coverImgUrl,
                cardWidth = LmmConsts.COVER_WIDTH,
                cardHeight = LmmConsts.COVER_HEIGHT,
            ),
            rows = getDetailRows(body = body),
        )
    }

    private fun getDetailRows(body: Element): List<MediaCardRow> {
        val rows = mutableListOf<MediaCardRow>()
        body.select("#site-content .detail section")
            .filter { it.selectFirst(".title-with-more") != null }
            .mapNotNull {
                LmmHtmlParser.parseSectionEl(it)
            }
            .forEach { rows.add(it) }
        body.selectFirst("#site-content .detail .right-sidebar")
            ?.let { LmmHtmlParser.parseCards(it) }
            ?.let {
                if (it.isNotEmpty()) {
                    rows.add(
                        MediaCardRow(
                            title = "推荐",
                            cardWidth = LmmConsts.CARD_WIDTH,
                            cardHeight = LmmConsts.CARD_HEIGHT,
                            list = it,
                        )
                    )
                }
            }
        return rows
    }

    override suspend fun getEpisodePlayInfo(
        playSource: MediaPlaySource,
        episode: MediaEpisode
    ): MediaHttpSource {
        if (!episode.id.startsWith("/play")) throw RuntimeException("不支持的地址")
        val url = "${lmmUrlService.getUrl()}${episode.id}"
        val body = url.toRequestBuild()
            .feignChrome(referer = episode.flag5)
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        LmmHtmlParser.checkMacMsg(body)
        val playerAAAAJson = PLAYER_AAAA_REGEX.find(body.outerHtml())
            ?.groups?.get(1)?.value ?: throw RuntimeException("解析地址失败 player_aaaa")
        var playerAAAA = LenientJson.decodeFromString<PlayerAAAA>(playerAAAAJson)
        if (playerAAAA.encrypt == 2) {
            playerAAAA = playerAAAA.copy(
                url = playerAAAA.url.decodeBase64ToStr(),
                urlNext = playerAAAA.urlNext.decodeBase64ToStr()
            )
        }
        return if (playerAAAA.url.endsWith(".m3u8", false)
            || playerAAAA.url.endsWith(".mp4", false)
        ) {
            MediaHttpSource(
                url = playerAAAA.url,
                httpHeaders = mapOf("Referrer" to url),
            )
        } else if (PLAYER_CONFIG_MAP.contains(playerAAAA.from)) {
            try {
                step2(
                    playerAAAA = playerAAAA,
                    referrer = url,
                )
            } catch (_: Throwable) {
                MediaSniffingSource(
                    url = playerAAAA.url,
                    httpHeaders = mapOf("Referrer" to url),
                )
            }
        } else {
            MediaSniffingSource(
                url = playerAAAA.url,
                httpHeaders = mapOf("Referrer" to url),
            )
        }
    }

    private suspend fun step2(playerAAAA: PlayerAAAA, referrer: String): MediaHttpSource {
        delay(200)
        var url = PLAYER_CONFIG_MAP[playerAAAA.from]
            ?.replace("{yunSite}", lmmUrlService.getYunSite())
            ?.replace("{url}", playerAAAA.url)
            ?.replace("{referer}", referrer)
            ?: throw RuntimeException("解析播放地址失败")
        val resp = url.toRequestBuild()
            .feignChrome(referer = referrer)
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
        val html = resp.parseHtml().outerHtml()
        url = resp.request.url.toString()
        return parseFromPostApi(iframeUrl = url, iframeHtml = html)
            ?: throw RuntimeException("无法解析播放地址")
    }

    private fun parseFromPostApi(
        iframeUrl: String,
        iframeHtml: String,
    ): MediaHttpSource? {
        val apiPath = POST_API_REGEX.find(iframeHtml)?.groups?.get(1)?.value ?: return null
        val vid = POST_VID_REGEX.find(iframeHtml)?.groups[1]?.value
            ?: throw RuntimeException("解析播放地址失败 vxdex vid")
        val t = POST_T_REGEX.find(iframeHtml)?.groups[1]?.value
            ?: throw RuntimeException("解析播放地址失败 vxdex t")
        val token = POST_TOKEN_REGEX.find(iframeHtml)?.groups[1]?.value
            ?: throw RuntimeException("解析播放地址失败 vxdex token")
        val act = POST_ACT_REGEX.find(iframeHtml)?.groups[1]?.value
            ?: throw RuntimeException("解析播放地址失败 vxdex act")
        val play = POST_PLAY_REGEX.find(iframeHtml)?.groups[1]?.value
            ?: throw RuntimeException("解析播放地址失败 vxdex play")
        val req = iframeUrl.toHttpUrl()
            .resolve(apiPath)
            .toString()
            .toRequestBuild()
            .feignChrome(referer = iframeUrl)
            .post(
                FormBody.Builder()
                    .add("vid", vid)
                    .add("t", t)
                    .add(
                        "token", token.decodeBase64()
                            .decryptAES128CBCPKCS7("ejjooopppqqqrwww", "1348987635684651")
                            .toString(Charsets.UTF_8)
                    )
                    .add("act", act)
                    .add("play", play)
                    .build()
            )
            .build()
        val respJson = okHttpClient.newCall(req)
            .execute()
            .checkSuccess()
            .stringBody()
        val resp = LenientJson.decodeFromString<VxdevResp>(respJson)
        if (resp.code != 200) {
            throw RuntimeException("解析播放地址失败," + resp.msg)
        }
        if (resp.ext == "link") {
            throw RuntimeException("解析播放地址失败, link")
        }
        return MediaHttpSource(
            url = resp.url,
            httpHeaders = if (resp.referer == "never") null else mapOf("Referrer" to iframeUrl),
        )
    }

    override suspend fun getEpisodeDanmakuDataList(episode: MediaEpisode): List<DanmakuData> =
        emptyList()
    override suspend fun getEpisodeDanmakuDataFlow(episode: MediaEpisode): DanmakuDataFlow? = null

    companion object {
        val PLAYER_AAAA_REGEX =
            "<script type=\"text/javascript\">var player_aaaa=(\\{.*?\\})</script>".toRegex()
        val PLAYER_CONFIG_MAP = mapOf(
            // dplayer
            // videojs
            // iva
            // iframe
            // link
            // swf
            // flv
            "dpmp4" to "https://{yunSite}/mp4hls/?type=mp4&vid={url}&referer={referer}",
            "dpwxv" to "https://{yunSite}/yunbox/?type=wxv&vid={url}&referer={referer}",
            "hls" to "https://{yunSite}/mp4hls/?type=hls&vid={url}&referer={referer}",
            "iqiyi" to "https://{yunSite}/iqiyi.php?url={url}",
            "migu" to "https://{yunSite}/yunbox/?type=migu&vid={url}&referer={referer}",
            "pptv" to "https://{yunSite}/yunbox/?type=pptv&vid={url}&referer={referer}",
            "qqcd" to "https://{yunSite}/yunbox/?type=qqcd&vid={url}&referer={referer}",
            "qqqy" to "https://{yunSite}/yunbox/?type=qqqy&vid={url}&referer={referer}",
            "qxyun" to "https://{yunSite}/yunbox/?type=qxyun&vid={url}&referer={referer}",
            "svod" to "https://{yunSite}/yunbox/?type=svod&vid={url}&referer={referer}",
            "tudou" to "https://{yunSite}/acfun58.php?id={url}&referer={referer}",
            "tv189" to "https://{yunSite}/189tv/?vid={url}&referer={referer}",
            "vxdev" to "https://{yunSite}/yunbox/?type=vxdev&vid={url}&referer={referer}",
            "xgvxcd" to "https://{yunSite}/yunbox/?type=vxcd&vid={url}&referer={referer}",
            "xigua" to "https://{yunSite}/189tv/?type=xgsp&vid={url}&referer={referer}",
            "ykbox" to "https://{yunSite}/404.php?host=4khls&vid={url}&referer={referer}",
        )
        val POST_API_REGEX = "\\$\\.post\\(\"(.*?)\",\\{\"vid\":vid,\"t\":t,\"token\":getc\\(token\\),\"act\":act,\"play\":play\\}".toRegex()
        val POST_VID_REGEX = "var vid = \"(.*?)\";".toRegex()
        val POST_T_REGEX = "var t = \"(.*?)\";".toRegex()
        val POST_TOKEN_REGEX = "var token = \"(.*?)\";".toRegex()
        val POST_ACT_REGEX = "var act = \"(.*?)\";".toRegex()
        val POST_PLAY_REGEX = "var play = \"(.*?)\";".toRegex()
    }
}