package it.bonificamarche.services.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import it.bonificamarche.services.R
import it.bonificamarche.services.common.show

class ForegroundStatusTransmissionPhotoService: Service() {

    override fun onBind(binder: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)



        createChannelNotification()
        //startNoticeService()
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
            "Photo status transmission",
            NotificationManager.IMPORTANCE_HIGH

        ).let {
            it.description = "Notifying the photo status transmission"
            it.enableLights(true)
            it.lightColor = Color.RED
            it
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create notice to notify the photo transmission status.
     * @param body: body of the notice.
     * @param imgToUpload: number of the image to uploaded.
     * @param progress: progress of status bar.
     */
    private fun createNotify(
        body: String,
        imgToUpload: Int,
        progress: Int,
    ): Notification {

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Invio foto")
            .setContentText(body)
            .setProgress(imgToUpload, progress, false)
            .setSmallIcon(R.drawable.photo_library_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Start service notice.
     */
    private fun startNoticeService(
        body : String,
        imgToUpload: Int,
        progress: Int
    ) {
        if (verbose) show(TAG, "Start notice service")

        val notification: Notification = createNotify(
            body,
            imgToUpload,
            progress
        )

        startForeground(ON_GOING_NOTIFICATION_ID, notification)
    }

    companion object {

        // Logging
        const val TAG = "Foreground Status Transmission Photo Service"
        const val verbose = true

        // Notification
        const val CHANNEL_ID = "Notifying the photo status transmission"
        const val ON_GOING_NOTIFICATION_ID = 1
    }
}