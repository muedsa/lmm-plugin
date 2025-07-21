package com.muedsa.tvbox.lmm.model

import kotlinx.serialization.Serializable

@Serializable
data class VerifyResult(
    val code: Int = -1,
    val msg: String = ""
)