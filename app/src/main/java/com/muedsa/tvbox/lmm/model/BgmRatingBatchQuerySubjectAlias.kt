package com.muedsa.tvbox.lmm.model

import kotlinx.serialization.Serializable

@Serializable
data class BgmRatingBatchQuerySubjectAlias(
    val name: String = "",
    val source: String = "",
)
