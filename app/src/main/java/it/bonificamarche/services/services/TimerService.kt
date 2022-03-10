package it.bonificamarche.services.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import it.bonificamarche.services.R
import it.bonificamarche.services.common.*
import java.util.*

open class TimerService : Service() {

    private var timer: Timer? = null
    var currentDate: String = getParsedDate(getLocalDate())

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

                val localDateTime = getLocalDateTime()
                val hour = localDateTime.hour
                val minute = localDateTime.minute
                show(TAG, "Hour: $hour, minute: $minute")

                when (hour) {
                    NOTICE_HOUR_PHOTO -> {
                        when (minute) {
                            NOTICE_MINUTE_PHOTO -> {
                                checkNoticePhoto(APP_NAME_COLTURE)
                            }
                        }
                    }
                }

//                if (flagForegroundServiceIsRunning) {
//                    // TODO specificare condizione di stop service (connessione con aidl)
//
//                    show(TAG, "Foreground service stopping...")
//                    stopService(Intent(this@TimerService, foregroundPhotoService::class.java))
//
//                    currentDate = addOneDay(currentDate)
//                    if (showLog) show(TAG, "Reset parameters. New date is $currentDate")
//                    flagForegroundServiceIsRunning = false
//                }
            }
        }

        // Start timer task
        timer = Timer()
        timer!!.schedule(timerTask, 0, PERIOD)
    }

    /**
     * Check if the conditions are successfully to notice the notify.
     */
    private fun checkNoticePhoto(appName : String) {

        val dateToCheck = getParsedDate(getLocalDate())
        val imgToSend = findPhotoToSend(appName)
        if (showLog) show(TAG, "Check photo in progress... Found: $imgToSend")

        if (compareDate(currentDate,  dateToCheck)) {

            if (imgToSend > 0) {

                if (showLog) show(TAG, "There are photos to send.!")

                if(!flagForegroundServiceIsRunning) {
                    // Start services
                    val intent = Intent(this, foregroundPhotoService::class.java)
                    intent.putExtra(getString(R.string.AppName), appName)

                    show(TAG, "Foreground service starting...")
                    startService(intent)
                    flagForegroundServiceIsRunning = true
                }
            }
        } else if (showLog) show(TAG, "Wait new date... for check photo. Current is $currentDate and check is $dateToCheck")
    }

    companion object {

        private const val PERIOD = 1000L

        // Notification Time
        private const val NOTICE_HOUR_PHOTO = 10
        private const val NOTICE_MINUTE_PHOTO = 8

        // App Name
        const val APP_NAME_COLTURE = "Colture"
        const val APP_NAME_IRRIGAZIONE = "Irrigazione"

        // Logging
        const val TAG = "Timer Service"
        const val showLog = true
    }
}