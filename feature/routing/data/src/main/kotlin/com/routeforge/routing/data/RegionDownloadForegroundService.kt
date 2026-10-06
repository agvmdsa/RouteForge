package com.routeforge.routing.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.routeforge.routing.domain.RegionDownloadController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject
import kotlin.math.roundToInt

private const val NOTIFICATION_CHANNEL_ID = "region_download_active"
private const val NOTIFICATION_ID = 2001

class RegionDownloadForegroundService : Service() {
    private val regionDownloadController: RegionDownloadController by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInForeground(buildNotification(contentText = getString(R.string.region_download_notification_title)))

        regionDownloadController.state
            .onEach { state ->
                if (state == null) {
                    stopSelf()
                } else {
                    updateNotification(state.region.displayName, state.progress)
                }
            }.launchIn(serviceScope)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.region_download_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification =
        Notification
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.region_download_notification_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

    private fun updateNotification(
        regionDisplayName: String,
        progress: Float?,
    ) {
        val contentText =
            if (progress != null) {
                getString(R.string.region_download_notification_text_progress, regionDisplayName, (progress * 100).roundToInt())
            } else {
                getString(R.string.region_download_notification_text_indeterminate, regionDisplayName)
            }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(contentText))
    }
}
