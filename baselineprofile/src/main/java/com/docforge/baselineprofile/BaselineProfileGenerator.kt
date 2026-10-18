package com.docforge.baselineprofile

import android.content.Intent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = TARGET_PACKAGE,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(TARGET_PACKAGE)
            }
        )

        device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE).depth(0)), UI_WAIT_MS)
        device.waitForIdle()

        openToolIfVisible("PDF Merge")
        device.pressBack()
        device.waitForIdle()

        openToolIfVisible("PDF Split/Extract")
        device.pressBack()
        device.waitForIdle()

        openToolIfVisible("Document Scanner")
        device.pressBack()
        device.waitForIdle()
    }

    private fun androidx.benchmark.macro.MacrobenchmarkScope.openToolIfVisible(label: String) {
        val toolNode = device.wait(Until.findObject(By.text(label)), UI_WAIT_MS) ?: return
        toolNode.click()
        device.waitForIdle()
    }

    companion object {
        private const val TARGET_PACKAGE = "com.docforge.app"
        private const val UI_WAIT_MS = 5_000L
    }
}
