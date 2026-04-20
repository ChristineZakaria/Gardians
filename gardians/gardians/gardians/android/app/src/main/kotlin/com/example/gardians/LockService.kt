package com.example.gardians

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.os.Handler
import android.os.Looper

class LockService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val lockRunnable = object : Runnable {
        override fun run() {
            lockDevice()
            handler.postDelayed(this, 2000) // كل ثانيتين
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, buildNotification())
        handler.post(lockRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(lockRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun lockDevice() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, AdminReceiver::class.java)
        if (dpm.isAdminActive(adminComponent)) {
            dpm.lockNow()
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "lock_channel"
        val channel = NotificationChannel(
            channelId,
            "Device Lock",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return Notification.Builder(this, channelId)
            .setContentTitle("Guardians")
            .setContentText("Device is blocked by parent")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }
}
