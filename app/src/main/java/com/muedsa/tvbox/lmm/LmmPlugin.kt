package com.muedsa.tvbox.lmm

import com.muedsa.tvbox.api.plugin.IPlugin
import com.muedsa.tvbox.api.plugin.PluginOptions
import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.api.service.IMediaCatalogService
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.api.store.IPluginPerfStore
import com.muedsa.tvbox.lmm.service.LmmUrlService
import com.muedsa.tvbox.lmm.service.MainScreenService
import com.muedsa.tvbox.lmm.service.MediaCatalogService
import com.muedsa.tvbox.lmm.service.MediaDetailService
import com.muedsa.tvbox.lmm.service.MediaSearchService
import com.muedsa.tvbox.tool.IPv6Checker
import com.muedsa.tvbox.tool.PluginCookieJar
import com.muedsa.tvbox.tool.SharedCookieSaver
import com.muedsa.tvbox.tool.createOkHttpClient

class LmmPlugin(tvBoxContext: TvBoxContext) : IPlugin(tvBoxContext = tvBoxContext) {

    private val store: IPluginPerfStore = tvBoxContext.store

    private val cookieSaver by lazy { SharedCookieSaver(store = store) }

    override var options: PluginOptions = PluginOptions(enableDanDanPlaySearch = true)

    private val okHttpClient by lazy {
        createOkHttpClient(
            debug = tvBoxContext.debug,
            cookieJar = PluginCookieJar(saver = cookieSaver),
            onlyIpv4 = tvBoxContext.iPv6Status != IPv6Checker.IPv6Status.SUPPORTED,
        )
    }
    private val lmmUrlService by lazy { LmmUrlService(okHttpClient = okHttpClient) }
    private val mainScreenService by lazy {
        MainScreenService(
            lmmUrlService = lmmUrlService,
            okHttpClient = okHttpClient,
        )
    }
    private val mediaDetailService by lazy {
        MediaDetailService(
            lmmUrlService = lmmUrlService,
            okHttpClient = okHttpClient,
        )
    }
    private val mediaSearchService by lazy {
        MediaSearchService(
            lmmUrlService = lmmUrlService,
            okHttpClient = okHttpClient,
        )
    }
    private val mediaCatalogService by lazy {
        MediaCatalogService(
            lmmUrlService = lmmUrlService,
            okHttpClient = okHttpClient,
        )
    }

    override fun provideMainScreenService(): IMainScreenService = mainScreenService

    override fun provideMediaDetailService(): IMediaDetailService = mediaDetailService

    override fun provideMediaSearchService(): IMediaSearchService = mediaSearchService

    override fun provideMediaCatalogService(): IMediaCatalogService = mediaCatalogService

    override suspend fun onInit() {}

    override suspend fun onLaunched() {}
}