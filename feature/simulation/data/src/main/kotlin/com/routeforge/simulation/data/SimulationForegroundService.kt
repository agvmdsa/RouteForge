package com.routeforge.simulation.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.routeforge.simulation.domain.SimulationController
import com.routeforge.simulation.domain.model.SimulationMode
import com.routeforge.simulation.domain.model.SimulationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

private const val NOTIFICATION_CHANNEL_ID = "simulation_active"
private const val NOTIFICATION_ID = 1001

class SimulationForegroundService : Service() {
    private val simulationController: SimulationController by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startInForeground(buildNotification(contentText = "Simulating movement along a route."))

        simulationController.session
            .onEach { session ->
                if (session == null) {
                    stopSelf()
                } else {
                    updateNotification(session.mode, session.status)
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
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Location simulation",
                NotificationManager.IMPORTANCE_LOW,
            )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification =
        Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("RouteForge is simulating your location")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

    private fun updateNotification(
        mode: SimulationMode,
        status: SimulationStatus,
    ) {
        val contentText =
            when {
                mode == SimulationMode.STATIONARY -> "Reporting a fixed simulated location."
                status == SimulationStatus.PAUSED -> "Route simulation paused."
                status == SimulationStatus.COMPLETED -> "Route simulation complete."
                else -> "Simulating movement along a route."
            }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(contentText))
    }
}
