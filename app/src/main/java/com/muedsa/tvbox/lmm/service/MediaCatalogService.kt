package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCatalogConfig
import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.api.data.MediaCatalogOptionItem
import com.muedsa.tvbox.api.data.PagingResult
import com.muedsa.tvbox.api.service.IMediaCatalogService
import com.muedsa.tvbox.lmm.LmmConsts
import com.muedsa.tvbox.lmm.LmmHtmlParser
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient
import timber.log.Timber

class MediaCatalogService(
    private val lmmUrlService: LmmUrlService,
    private val smartVerifyService: SmartVerifyService,
    private val okHttpClient: OkHttpClient,
) : IMediaCatalogService {

    override suspend fun getConfig(): MediaCatalogConfig {
        val doc = "${lmmUrlService.getUrl()}/vod/search/by/time/page/1.html".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
        val body = doc.body()
        LmmHtmlParser.checkMacMsg(body)
        if (smartVerifyService.checkNeedValid(doc) && !smartVerifyService.verify()) {
            throw RuntimeException("网站需要验证码，但尝试识别验证码失败，重试操作再次尝试验证")
        }
        return MediaCatalogConfig(
            initKey = "1",
            pageSize = 20,
            cardWidth = LmmConsts.CARD_WIDTH,
            cardHeight = LmmConsts.CARD_HEIGHT,
            catalogOptions = listOf(
                MediaCatalogOption(
                    name = "排序",
                    value = "by",
                    items = listOf(
                        MediaCatalogOptionItem(
                            name = "最近更新",
                            value = "time",
                            defaultChecked = true
                        ),
                        MediaCatalogOptionItem(
                            name = "最高人气",
                            value = "hits",
                        ),
                        MediaCatalogOptionItem(
                            name = "最高评分",
                            value = "score",
                        ),
                        MediaCatalogOptionItem(
                            name = "最多点赞",
                            value = "up",
                        )
                    ),
                    required = true
                ),
                MediaCatalogOption(
                    name = "过滤",
                    value = "filterByName",
                    items = listOf(
                        MediaCatalogOptionItem(
                            name = "动态漫",
                            value = "动态漫",
                            defaultChecked = true
                        ),
                        MediaCatalogOptionItem(
                            name = "AI漫剧",
                            value = "AI漫剧",
                            defaultChecked = true
                        ),
                    ),
                    required = false,
                    multiple = true,
                ),
            )
        )
    }

    override suspend fun catalog(
        options: List<MediaCatalogOption>,
        loadKey: String,
        loadSize: Int
    ): PagingResult<MediaCard> {
        val page = loadKey.toInt()
        val by = options.find { option -> option.value == "by" }?.items[0]?.value
            ?: throw RuntimeException("排序为必选项")
        val filterNames =
            options.find { option -> option.value == "filterByName" }?.items?.map { it.value }
        val body = "${lmmUrlService.getUrl()}/vod/search/by/$by/page/$page.html".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        LmmHtmlParser.checkMacMsg(body)
        var list = body.selectFirst("#site-content #list_videos_common_videos_list #mdym")
            ?.let { LmmHtmlParser.parseCards(it) }
            ?: emptyList()
        val pageLiEls =
            body.select("#site-content #list_videos_common_videos_list ul.pagination li.page-item")
        val currentLiEl = pageLiEls.find { it.selectFirst(">span.page-link.active") != null }
        val nextKey = currentLiEl?.nextElementSibling()?.selectFirst(">a.page-link")?.text()?.trim()
        if (!filterNames.isNullOrEmpty() && list.isNotEmpty()) {
            list = list.filter { c -> !filterNames.any { n -> c.title.contains(n) } }
            if (list.isEmpty() && nextKey != null) {
                return catalog(options = options, loadKey = nextKey, loadSize = loadSize)
            }
        }
        Timber.i("catalog page=$page nextKey=$nextKey ")
        return PagingResult(
            list = list,
            nextKey = nextKey,
            prevKey = if (page > 1) "${page - 1}" else null
        )
    }
}