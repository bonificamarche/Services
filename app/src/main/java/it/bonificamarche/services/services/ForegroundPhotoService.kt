package it.bonificamarche.services.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import it.bonificamarche.services.R
import it.bonificamarche.services.StartExternalApplication
import it.bonificamarche.services.common.show
import java.util.*


class ForegroundPhotoService : Service() {

    private var appName : String? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val bundle = intent?.extras
        appName = bundle?.getString(getString(R.string.AppName))

        createChannelNotification()
        startNoticeService()

        return START_NOT_STICKY
    }

    /**
     * Create channel to notification.
     */
    private fun createChannelNotification() {

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Notify Photo",
            NotificationManager.IMPORTANCE_HIGH

        ).let {
            it.description = "Foreground Notify Photo"
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
        if (showLog) show(TAG, "Start notice service")

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val vibrate = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)

        val notificationIntent = Intent(applicationContext, StartExternalApplication::class.java)
        notificationIntent.putExtra("package", "it.bonificamarche.${appName?.toLowerCase(Locale.ROOT)}")
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        val contentIntent = PendingIntent.getActivity(
            this,
            ON_GOING_NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foto da inviare")
                .setContentText("Ci sono foto da inviare nell'app ${appName}!")
                .setSmallIcon(R.drawable.warning)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSound(sound)
                .setVibrate(vibrate)
                .setContentIntent(contentIntent)
                .setChannelId(CHANNEL_ID)
                .build()

        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        notification.flags = notification.flags or Notification.DEFAULT_VIBRATE
        notification.flags = notification.flags or Notification.FLAG_AUTO_CANCEL
        startForeground(ON_GOING_NOTIFICATION_ID, notification)
    }

    companion object {

        const val CHANNEL_ID = "Foreground Service"
        private const val ON_GOING_NOTIFICATION_ID = 1

        const val showLog = true
        const val TAG = "Foreground service"
    }
}