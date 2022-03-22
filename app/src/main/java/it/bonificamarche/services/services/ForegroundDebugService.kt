package it.bonificamarche.services.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import it.bonificamarche.services.R
import it.bonificamarche.services.common.show

class ForegroundDebugService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        show(TAG, "Started!")

        createChannelNotification()
        startNoticeService()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        show(TAG, "Destroy!")
        this.stopSelf()
    }

    /**
     * Create channel to notification.
     */
    private fun createChannelNotification() {
        show(TAG, "Create channel notification")

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Debug Service",
            NotificationManager.IMPORTANCE_DEFAULT

        ).let {
            it.description = "Debug Service"
            it.enableLights(true)
            it.lightColor = Color.RED
            it
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Start service notice.
     */
    private fun startNoticeService() {
        show(TAG, "Start notice service")

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val vibrate = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Debug notice")
                .setContentText("Il server locale è in funzione.")
                .setSmallIcon(R.drawable.warning)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(sound)
                .setVibrate(vibrate)
                .setChannelId(CHANNEL_ID)
                .build()

        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        notification.flags = notification.flags or Notification.DEFAULT_VIBRATE
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        startForeground(ON_GOING_NOTIFICATION_ID, notification)
    }

    companion object {

        const val CHANNEL_ID = "Debug Service"
        private const val ON_GOING_NOTIFICATION_ID = 2

        const val TAG = "Debug service"
    }
}