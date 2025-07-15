package com.muedsa.tvbox.lmm.model

import kotlinx.serialization.Serializable

@Serializable
data class VxdevResp(
    val code: Int = 0,
    val msg: String = "",
    val ext: String = "",
    val referer: String = "",
    val url: String = "",
)
