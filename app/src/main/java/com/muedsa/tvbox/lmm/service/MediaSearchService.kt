package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.lmm.LmmConsts
import com.muedsa.tvbox.lmm.LmmHtmlParser
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MediaSearchService(
    private val lmmUrlService: LmmUrlService,
    private val okHttpClient: OkHttpClient,
) : IMediaSearchService {

    override suspend fun searchMedias(query: String): MediaCardRow {
        val body = "${lmmUrlService.getUrl()}/vod/search.html?wd=$query".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        LmmHtmlParser.checkMacMsg(body)
        val list = body.selectFirst("#site-content #list_videos_common_videos_list #mdym")
            ?.let { LmmHtmlParser.parseCards(it) }
            ?: emptyList()
        return MediaCardRow(
            title = "search list",
            cardWidth = LmmConsts.CARD_WIDTH,
            cardHeight = LmmConsts.CARD_HEIGHT,
            list = list,
        )
    }
}