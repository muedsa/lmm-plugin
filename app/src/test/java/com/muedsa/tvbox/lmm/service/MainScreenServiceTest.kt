package com.muedsa.tvbox.lmm.service

import com.muedsa.tvbox.lmm.TestPlugin
import com.muedsa.tvbox.lmm.checkMediaCardRows
import kotlinx.coroutines.test.runTest
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainScreenServiceTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun init() {
            ShadowLog.stream = System.out
        }
    }

    private val service = TestPlugin.provideMainScreenService()

    @Test
    fun getRowsDataTest() = runTest{
        val rows = service.getRowsData()
        checkMediaCardRows(rows = rows)
    }

}