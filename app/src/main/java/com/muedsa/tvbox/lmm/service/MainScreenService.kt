package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.lmm.LmmHtmlParser
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MainScreenService(
    private val lmmUrlService: LmmUrlService,
    private val bgmRatingQueryService: BgmRatingQueryService,
    private val okHttpClient: OkHttpClient,
) : IMainScreenService {

    override suspend fun getRowsData(): List<MediaCardRow> {
        val body = lmmUrlService.getUrl()
            .toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        LmmHtmlParser.checkMacMsg(body)
        var rows = body.select("#site-content > .container > section.pb-e-lg-40")
            .mapNotNull { LmmHtmlParser.parseSectionEl(it) }
        if (rows.isNotEmpty()) {
            val matchedResults = BgmRatingQueryService.loopBatchQuery(
                medias = rows.flatMap { it.list },
                service = bgmRatingQueryService,
            )
            if (matchedResults.isNotEmpty()) {
                rows = rows.map {
                    it.copy(list = it.list.map { c ->
                        val result = matchedResults[c.id]
                        if (result != null
                            && result.rating?.score != null
                        ) {
                            MediaCard(
                                id = c.id,
                                title = c.title,
                                detailUrl = c.detailUrl,
                                subTitle = c.subTitle.orEmpty() + " | ★${result.rating.score}",
                                coverImageUrl = c.coverImageUrl,
                            )
                        } else c
                    })
                }
            }
        }
        return rows
    }
}