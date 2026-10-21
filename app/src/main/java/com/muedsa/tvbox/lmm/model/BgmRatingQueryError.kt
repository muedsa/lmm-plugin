package com.muedsa.tvbox.lmm.model

import kotlinx.serialization.Serializable

@Serializable
data class BgmRatingQueryError(
    val code: String = "",
    val message: String = "",
)
