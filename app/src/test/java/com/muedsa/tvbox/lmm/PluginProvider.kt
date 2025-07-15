package com.muedsa.tvbox.lmm

import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.tool.IPv6Checker
import com.muedsa.tvbox.tool.PluginCookieJar
import com.muedsa.tvbox.tool.SharedCookieSaver
import com.muedsa.tvbox.tool.createOkHttpClient
import kotlin.getValue

val TestPluginPrefStore by lazy {
    FakePluginPrefStore()
}

val TestCookieSaver by lazy {
    SharedCookieSaver(store = TestPluginPrefStore)
}

val TestPluginCookieJar by lazy {
    PluginCookieJar(saver = TestCookieSaver)
}

val TestOkHttpClient by lazy {
    createOkHttpClient(
        debug = true,
        cookieJar = TestPluginCookieJar,
        onlyIpv4 = true,
    )
}

val TestPlugin by lazy {
    LmmPlugin(
        tvBoxContext = TvBoxContext(
            screenWidth = 1920,
            screenHeight = 1080,
            debug = true,
            store = TestPluginPrefStore,
            iPv6Status = IPv6Checker.checkIPv6Support()
        )
    )
}