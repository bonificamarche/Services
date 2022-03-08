package it.bonificamarche.services.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import it.bonificamarche.services.common.*
import java.util.*


open class CheckPhotoService : Service() {

    private var timer: Timer? = null
    var currentDate: Int = currentDate()

    private lateinit var foregroundPhotoService: ForegroundPhotoService
    private var flagForegroundServiceIsRunning = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        foregroundPhotoService = ForegroundPhotoService()

        startTimer()
        return START_STICKY
    }

    /**
     * Start timer task.
     */
    private fun startTimer() {

        if (showLog) show(TAG, "Start timer task!")
        val timerTask = object : TimerTask() {

            override fun run() {
                checkNoticePhoto()
            }
        }

        // Start timer task
        timer = Timer()
        timer!!.schedule(timerTask, 0, PERIOD)
    }

    /**
     * Check if the conditions are successfully to notice the notify.
     * @return true if the notice (to send photo) must be notify, false otherwise.
     */
    private fun checkNoticePhoto() {

        val dateToCheck = currentDate()

        val imgToSend = findPhotoToSend(this)
        if (showLog) show(TAG, "Check photo in progress... Found: $imgToSend")

        if (flagForegroundServiceIsRunning) {
            show(TAG, "Foreground service stopping...")
            stopService(Intent(this@CheckPhotoService, foregroundPhotoService::class.java))

            currentDate = addOneDay(currentDate)
            if (showLog) show(TAG, "Reset parameters. New date is $currentDate")
            flagForegroundServiceIsRunning = false
        }

        if (currentDate == dateToCheck) { // TODO Check this

            if (imgToSend == 0) {

                val currentHour = currentHour().substring(0, 2).toInt()
                val currentMinutes = currentHour().substring(2, 4).toInt()
                if (showLog) show(
                    TAG,
                    "There are photos to send. Hour: $currentHour, minutes: $currentMinutes"
                )

                if (currentHour == NOTICE_HOUR.toInt() && currentMinutes == NOTICE_MINUTE.toInt()) {
                    if (showLog) show(TAG, "Need to send Photo!")

                    // Start services
                    startService(Intent(this, foregroundPhotoService::class.java))
                    flagForegroundServiceIsRunning = true
                }
            }
        } else if (showLog) show(TAG, "Wait new date... for check photo")
    }

    companion object {

        private const val PERIOD = 60000L
        private const val NOTICE_HOUR = "11"
        private const val NOTICE_MINUTE = "52"

        // Logging
        const val TAG = "Check Photo Service"
        const val showLog = true
    }
}