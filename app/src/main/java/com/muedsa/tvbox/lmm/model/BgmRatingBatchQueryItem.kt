package com.muedsa.tvbox.lmm.model

import kotlinx.serialization.Serializable

@Serializable
data class BgmRatingBatchQueryItem(
    val inputId: String,
    val names: List<String>,
)
