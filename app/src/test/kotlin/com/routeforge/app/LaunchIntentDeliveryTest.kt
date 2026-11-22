package com.routeforge.app

import android.content.Intent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LaunchIntentDeliveryTest {
    @Test
    fun `a first start with an ordinary intent is fresh`() {
        assertTrue(isFreshLaunchIntent(isRecreated = false, intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK))
        assertTrue(isFreshLaunchIntent(isRecreated = false, intentFlags = 0))
    }

    @Test
    fun `an activity rebuilt by the system is not fresh`() {
        assertFalse(isFreshLaunchIntent(isRecreated = true, intentFlags = 0))
    }

    @Test
    fun `a task reopened from the recent apps is not fresh`() {
        assertFalse(isFreshLaunchIntent(isRecreated = false, intentFlags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY))
        assertFalse(
            isFreshLaunchIntent(
                isRecreated = false,
                intentFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
            ),
        )
    }

    @Test
    fun `a rebuilt activity reopened from the recent apps is not fresh`() {
        assertFalse(isFreshLaunchIntent(isRecreated = true, intentFlags = Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY))
    }
}
