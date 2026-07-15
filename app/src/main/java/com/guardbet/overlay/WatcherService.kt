package com.guardbet.overlay

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class WatcherService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isOverlayShown = false
    private val pollIntervalMs = 1500L

    private val targetPackages = setOf(
        "org.xbet.client1",
        "com.xbet.client1",
        "com.onexbet.client"
    )

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, pollIntervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForegroundApp() {
        val currentApp = getForegroundPackage()
        val shouldShow = currentApp != null && targetPackages.contains(currentApp)

        if (shouldShow && !isOverlayShown) {
            isOverlayShown = true
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
        } else if (!shouldShow && isOverlayShown) {
            isOverlayShown = false
            stopService(Intent(this, OverlayService::class.java))
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 10_000
        val events = usm.queryEvents(begin, end)
        var lastPackage: String? = null
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }

    private fun buildNotification(): Notification {
        val channelId = "guardbet_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "GuardBet surveillance",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("GuardBet actif")
            .setContentText("Repères de probabilités et suivi de budget activés")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    companion object {
        const val NOTIF_ID = 1001
    }
}
