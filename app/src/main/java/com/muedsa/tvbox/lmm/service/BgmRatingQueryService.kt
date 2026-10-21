package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.MediaBase
import com.muedsa.tvbox.lmm.model.BgmRatingBatchQueryItem
import com.muedsa.tvbox.lmm.model.BgmRatingBatchQueryReq
import com.muedsa.tvbox.lmm.model.BgmRatingBatchQueryResp
import com.muedsa.tvbox.lmm.model.BgmRatingBatchQueryResult
import retrofit2.http.Body
import retrofit2.http.POST

interface BgmRatingQueryService {

    @POST("api/v1/ratings/batch")
    suspend fun batchQuery(@Body req: BgmRatingBatchQueryReq): BgmRatingBatchQueryResp


    companion object {

        const val MAX_BATCH_QUERY_ITEM_NUM = 20

        suspend fun loopBatchQuery(
            medias: List<MediaBase>,
            service: BgmRatingQueryService
        ): Map<String, BgmRatingBatchQueryResult> {
            val chunkedItems = medias.map {
                BgmRatingBatchQueryItem(
                    inputId = it.id,
                    names = listOf(it.title)
                )
            }.distinctBy { it.inputId }
                .chunked(MAX_BATCH_QUERY_ITEM_NUM)
            return chunkedItems.map { items ->
                try {
                    val resp = service.batchQuery(
                        req = BgmRatingBatchQueryReq(items = items)
                    )
                    return resp.results?.filter {
                        it.status == "matched" && it.rating != null
                    }?.associateBy { it.inputId } ?: emptyMap()
                } catch (_: Throwable) {
                    emptyMap<String, BgmRatingBatchQueryResult>()
                }
            }.flatMap { it.entries }
                .associate { it.key to it.value }
        }
    }
}