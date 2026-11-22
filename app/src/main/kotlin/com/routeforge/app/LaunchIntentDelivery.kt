package com.routeforge.app

import android.content.Intent

/**
 * Whether the intent an activity starts with is something the user just asked for. The system hands the same intent over again
 * when it rebuilds the activity and when the task is reopened from the recent apps; acting on it then would repeat, with no
 * request from the user, what an earlier launch already did.
 */
internal fun isFreshLaunchIntent(
    isRecreated: Boolean,
    intentFlags: Int,
): Boolean = !isRecreated && (intentFlags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == 0
