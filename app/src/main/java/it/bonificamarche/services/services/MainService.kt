package it.bonificamarche.services.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.bonificamarche.services.R
import it.bonificamarche.services.common.*
import java.lang.Exception
import java.util.*

open class MainService : Service() {

    private val binder = LocalBinder()

    private var timer: Timer? = null
    var currentDate: String = getParsedDate(getLocalDate())

    private lateinit var foregroundPhotoService: ForegroundPhotoService
    private var flagForegroundServiceIsRunning = false

    inner class LocalBinder : Binder() {
        fun getService(): MainService = this@MainService
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        foregroundPhotoService = ForegroundPhotoService()

        // Local Broadcast receiver to communicate
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                mainServiceReceiver,
                IntentFilter(getString(R.string.communicationFromAidlServerServiceToMainService))
            )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        show(TAG, "Started!")

        startTimer()
        return START_STICKY
    }

    /**
     * Start timer task.
     */
    private fun startTimer() {

        show(TAG, "[Time Task] Start timer task!")
        val timerTask = object : TimerTask() {

            override fun run() {

                val localDateTime = getLocalDateTime()
                val hour = localDateTime.hour
                val minute = localDateTime.minute
                show(TAG, "[Time Task] Hour: $hour, minute: $minute")

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
    private fun checkNoticePhoto(appName: String) {

        val dateToCheck = getParsedDate(getLocalDate())
        val imgToSend = findPhotoToSend(appName)
        if (verbose) show(TAG, "[Check Photo] in progress... Found: $imgToSend")

        if (compareDate(currentDate, dateToCheck)) {

            if (imgToSend > 0) {

                if (verbose) show(TAG, "[Check Photo] There are photos to send.!")

                if (!flagForegroundServiceIsRunning) {
                    // Start services
                    val intent = Intent(this, foregroundPhotoService::class.java)
                    intent.putExtra(getString(R.string.AppName), appName)

                    show(TAG, "[Check Photo] Foreground service starting...")
                    startService(intent)
                    flagForegroundServiceIsRunning = true
                }
            }
        } else if (verbose) show(
            TAG,
            "[Check Photo] Wait new date... for check photo. Current is $currentDate and check is $dateToCheck"
        )
    }

    /**
     * Send photo to remote server.
     * @param path: path of the root folder that contains the photos to be sent.
     */
    private fun sendPhotoToRemoteServer(path : String) {
        for (i in 0 until 5) {
            Thread.sleep(2000)
            sendMessageToAidlServer(Actions.NOTIFY_CLIENTS, "Sent photo ${i + 1}")
        }
    }

    /**
     * Local receiver to communicate from aidl server service to main service.
     */
    private val mainServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            val action = intent.getSerializableExtra(context.getString(R.string.action)) as Actions
            val message = bundle?.getString(context.getString(R.string.message))!!
            show(TAG, "[AIDL Server --> Main Service] Action: $action, message: $message.")

            when (action) {
                Actions.SEND_PHOTO -> {
                    sendPhotoToRemoteServer(message)
                }
                else -> throw Exception("Actions not implemented!")
            }
        }
    }

    /**
     * Call this function when the main service needs to communicate with the server service.
     * @param message: content of the message.
     */
    private fun sendMessageToAidlServer(action: Actions, message: String = "") {
        val intent = Intent(getString(R.string.communicationFromMainServiceToAidlServerService))
        intent.putExtra(getString(R.string.action), action)
        intent.putExtra(getString(R.string.message), message)
        show(TAG, "[Main Service --> AIDL Server]  Action: $action, message: $message")

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
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
        const val TAG = "Main Service"
        const val verbose = true
    }
}