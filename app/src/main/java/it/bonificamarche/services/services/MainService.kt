package it.bonificamarche.services.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.bonificamarche.services.Actions
import it.bonificamarche.services.R
import it.bonificamarche.services.aidl.Action
import it.bonificamarche.services.aidl.AidlServerServiceImpl
import it.bonificamarche.services.aidl.Photo
import it.bonificamarche.services.aidl.Transmission
import it.bonificamarche.services.common.*
import java.util.*

open class MainService : Service() {

    private val binder = LocalBinder()

    // Timer
    private var timer: Timer? = null
    var currentDate: String = getParsedDate(getLocalDate())

    // Foreground photo
    private lateinit var foregroundCheckPhotoService: ForegroundCheckPhotoService
    private var foregroundCheckPhotoServiceIsRunning = false

    // Foreground Status Transmission pPhoto
    private lateinit var foregroundStatusTransmissionService: ForegroundStatusTransmissionPhotoService

    inner class LocalBinder : Binder() {
        fun getService(): MainService = this@MainService
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        foregroundCheckPhotoService = ForegroundCheckPhotoService()
        foregroundStatusTransmissionService = ForegroundStatusTransmissionPhotoService()

        // Local Broadcast receiver to communicate
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                mainServiceReceiverFromAidl,
                IntentFilter(getString(R.string.communicationFromAidlServerService))
            )

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                mainServiceReceiverFromForeground,
                IntentFilter(getString(R.string.communicationForegroundSendPhoto))
            )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (verbose) show(TAG, "Started!")

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

                if (!foregroundCheckPhotoServiceIsRunning) {
                    // Start service
                    val intent = Intent(this, foregroundCheckPhotoService::class.java)
                    intent.putExtra(getString(R.string.AppName), appName)

                    show(TAG, "[Check Photo] Foreground service starting...")
                    startService(intent)
                    foregroundCheckPhotoServiceIsRunning = true
                }
            }
        } else if (verbose) show(
            TAG,
            "[Check Photo] Wait new date... for check photo. Current is $currentDate and check is $dateToCheck"
        )
    }

    /**
     * Local receiver to communicate from aidl server service to main service.
     */
    private val mainServiceReceiverFromAidl: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras!!
            val action = intent.getSerializableExtra(context.getString(R.string.action)) as Actions
            val message = bundle.getString(context.getString(R.string.message))!!
            val idUser = bundle.getInt(context.getString(R.string.id_user))

            show(TAG, "[AIDL Server --> Main Service] Received Action: $action, message: $message")

            when (action) {
                Actions.START_SEND_PHOTO -> {

                    // Start service
                    val intentToForeground =
                        Intent(this@MainService, foregroundStatusTransmissionService::class.java)

                    intentToForeground.putExtra(getString(R.string.id_user), idUser)
                    intentToForeground.putExtra(getString(R.string.message), message)
                    startService(intentToForeground)
                    sendPhotoInRunning = true
                }

                Actions.STOP_SEND_PHOTO -> {
                    show(TAG, "Updated flag sendPhotoInRunning...")
                    sendPhotoInRunning = false
                }
                else -> throw Exception("Actions not implemented!")
            }
        }
    }

    /**
     * Local receiver to communicate from foreground service to main service.
     */
    private val mainServiceReceiverFromForeground: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.getParcelableExtra<Action>(context.getString(R.string.action))!!

            show(AidlServerServiceImpl.TAG, "[Foreground --> Main Service] Received Action: $action")

            when (action.action) {
                Actions.ERROR_SEND_PHOTO -> {
                    show(TAG, "Updated flag sendPhotoInRunning...")
                    sendPhotoInRunning = false
                }
                else -> throw Exception("Actions not implemented!")
            }
        }
    }

    companion object {

        private const val PERIOD = 1000L

        // Notification Photo Time
        private const val NOTICE_HOUR_PHOTO = 10
        private const val NOTICE_MINUTE_PHOTO = 8

        // Notification Debug time
        private const val DIFF_MINUTES_DEBUG = 200

        // App Name
        private const val APP_NAME_COLTURE = "Colture"
        private const val APP_NAME_IRRIGAZIONE = "Irrigazione"

        // Logging
        private const val TAG = "Main Service"
        private const val verbose = true

        // Manage service
        private var sendPhotoInRunning = false
    }
}