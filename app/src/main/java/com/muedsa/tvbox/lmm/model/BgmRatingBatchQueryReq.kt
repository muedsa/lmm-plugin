package com.muedsa.tvbox.lmm.model

import kotlinx.serialization.Serializable

@Serializable
data class BgmRatingBatchQueryReq(
    val items: List<BgmRatingBatchQueryItem>
)
