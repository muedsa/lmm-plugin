package com.muedsa.tvbox.lmm

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import org.jsoup.nodes.Element

object LmmHtmlParser {
    fun parseSectionEl(el: Element): MediaCardRow? {
        val title = el.selectFirst(".title-with-more .title-box .h3-md")?.text() ?: return null
        val cards = parseCards(el)
        if (cards.isEmpty()) return null
        return MediaCardRow(
            title = title,
            cardWidth = LmmConsts.CARD_WIDTH,
            cardHeight = LmmConsts.CARD_HEIGHT,
            list = cards,
        )
    }

    fun parseCards(el: Element): List<MediaCard> {
        return el.select(".video-img-box").mapNotNull { boxEl ->
            val imgEl = boxEl.selectFirst(".img-box img")
            val detailUrl = boxEl.selectFirst(".detail h6 a")?.attr("href")
            val cardTitle = boxEl.selectFirst(".detail h6 a")?.text()?.trim()
            val cardSubTitle = boxEl.selectFirst(".img-box a .absolute-bottom-right .label")?.text()?.trim()
            if (imgEl != null && detailUrl != null && cardTitle != null) {
                MediaCard(
                    id = detailUrl,
                    title = cardTitle,
                    detailUrl = detailUrl,
                    subTitle = cardSubTitle,
                    coverImageUrl = imgEl.attr("data-src")
                )
            } else null
        }
    }

    fun checkMacMsg(body: Element) {
        val msgEl = body.selectFirst(".mac_msg_jump") ?: return
        val msgArr = mutableListOf<String>()
        msgEl.selectFirst(".msg_jump_tit")?.text()?.trim()?.let { msgArr.add(it) }
        msgEl.selectFirst(".title")?.text()?.trim()?.let { msgArr.add(it) }
        msgEl.selectFirst(".text")?.text()?.trim()?.let { msgArr.add(it) }
        throw RuntimeException(msgArr.joinToString("\n"))
    }
}