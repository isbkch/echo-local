package com.isbkch.echolocal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppIdentityTest {
    @Test
    fun applicationUsesEcholocalIdentity() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("com.isbkch.echolocal", context.packageName)
        assertEquals("Echolocal", context.getString(R.string.app_name))
    }
}
