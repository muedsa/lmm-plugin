package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.lmm.TestPlugin
import com.muedsa.tvbox.lmm.checkMediaCard
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaCatalogServiceTest {

    private val service = TestPlugin.provideMediaCatalogService()

    @Test
    fun getConfig_test() = runTest {
        val config = service.getConfig()
        check(config.pageSize > 0)
        check(config.catalogOptions.isNotEmpty())
        check(config.catalogOptions.size == config.catalogOptions.distinctBy { it.value }.size)
        for (option in config.catalogOptions) {
            check(option.items.isNotEmpty())
            check(option.items.size == option.items.distinctBy { it.value }.size)
        }
        check(config.cardWidth > 0)
    }

    @Test
    fun catalog_test() = runTest {
        val config = service.getConfig()
        val pagingResult = service.catalog(
            options = MediaCatalogOption.getDefault(config.catalogOptions),
            loadKey = config.initKey,
            loadSize = config.pageSize
        )
        check(pagingResult.list.isNotEmpty())
        pagingResult.list.forEach {
            checkMediaCard(it, config.cardType)
        }
    }

}