package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.lmm.TestPlugin
import com.muedsa.tvbox.lmm.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaSearchServiceTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            ShadowLog.stream = System.out
        }
    }

    private val service = TestPlugin.provideMediaSearchService()

    @Before
    fun setUpDelay() {
        Thread.sleep(3333)
    }

    @Test
    fun searchMedias_test() = runTest {
        val row = service.searchMedias("GIRLS BAND CRY")
        checkMediaCardRow(row = row)
    }
}