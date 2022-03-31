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
import it.bonificamarche.services.common.*
import java.util.*

open class MainService : Service() {

    private val binder = LocalBinder()

    // Timer
    private var timer: Timer? = null
    var cropAppDate: String = getParsedDate(getLocalDate())
    var irrigationAppDate: String = getParsedDate(getLocalDate())

    // Foreground photo
    private lateinit var foregroundCheckPhotoService : ForegroundCheckPhotoService
    private var cropForegroundServiceIsRunning = false
    private var irrigationForegroundServiceIsRunning = false

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

        if(verbose)  show(TAG, "[Time Task] Start timer task!")
        val timerTask = object : TimerTask() {

            override fun run() {

                val localDateTime = getLocalDateTime()
                val hour = localDateTime.hour
                val minute = localDateTime.minute
                if(verbose)  show(TAG, "[Time Task] Hour: $hour, minute: $minute")

                when (hour) {
                    NOTICE_HOUR_PHOTO -> {
                        when (minute) {
                            START_CROP_NOTICE_MINUTE_PHOTO -> {
                                startCheckNoticePhoto(CROP_APP_NAME, cropAppDate)
                            }

                            STOP_CROP_NOTICE_MINUTE_PHOTO -> {
                                cropAppDate = stopCheckNoticePhoto(CROP_APP_NAME, cropAppDate)
                            }

                            START_IRRIGATION_NOTICE_MINUTE_PHOTO -> {
                                startCheckNoticePhoto(IRRIGATION_APP_NAME, irrigationAppDate)
                            }

                            STOP_IRRIGATION_NOTICE_MINUTE_PHOTO -> {
                                irrigationAppDate = stopCheckNoticePhoto(IRRIGATION_APP_NAME, irrigationAppDate)
                            }
                        }
                    }
                }
            }
        }

        // Start timer task
        timer = Timer()
        timer!!.schedule(timerTask, 0, PERIOD)
    }

    /**
     * Check if the conditions are successfully to notify that there are photos to send.
     * If true start the service.
     */
    private fun startCheckNoticePhoto(appName: String, date: String) {

        val dateToCheck = getParsedDate(getLocalDate())

        var flag = if (appName == IRRIGATION_APP_NAME)
            irrigationForegroundServiceIsRunning
        else
            cropForegroundServiceIsRunning

        val imgToSend = findPhotoToSend(appName)
        if (verbose) show(TAG, "[Check Photo] in progress... Found: $imgToSend for $appName")

        if (compareDate(date, dateToCheck) && !sendPhotoInRunning) {

            if (imgToSend > 0) {

                if (verbose) show(TAG, "[Check Photo] There are photos to send for $appName")

                if (!flag) {
                    // Start services
                    val intent = Intent(this, foregroundCheckPhotoService::class.java)
                    intent.putExtra(getString(R.string.AppName), appName)

                    if(verbose)  show(TAG, "[Check Photo] Foreground service starting... for $appName")
                    startService(intent)
                    flag = true
                }
            }
        } else if (verbose) show(
            TAG,
            "[Check Photo] Wait new date or service send photo is in running. Current is $date and check is $dateToCheck for $appName"
        )

        if (appName == IRRIGATION_APP_NAME)
            irrigationForegroundServiceIsRunning = flag
        else
            cropForegroundServiceIsRunning = flag
    }

    /**
     * Stop the foreground photo service.
     */
    private fun stopCheckNoticePhoto(appName: String, date: String): String {

        val newDate = addOneDay(date)

        if (cropForegroundServiceIsRunning || irrigationForegroundServiceIsRunning) {
            if (verbose) show(TAG, "Reset parameters. New date is $newDate for $appName")

            stopService(Intent(this@MainService, foregroundCheckPhotoService ::class.java))
            if (verbose) show(TAG, "Foreground service stopping... for $appName")


            if (appName == IRRIGATION_APP_NAME)
                irrigationForegroundServiceIsRunning = false
            else
                cropForegroundServiceIsRunning = false
        }

        return newDate
    }

    /**
     * Local receiver to communicate from aidl server service to main service.
     */
    private val mainServiceReceiverFromAidl: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras!!
            val action =
                intent.getSerializableExtra(context.getString(R.string.action)) as Actions
            val message = bundle.getString(context.getString(R.string.message))!!
            val idUser = bundle.getInt(context.getString(R.string.id_user))

            if(verbose)  show(
                TAG,
                "[AIDL Server --> Main Service] Received Action: $action, message: $message"
            )

            when (action) {
                Actions.START_SEND_PHOTO -> {

                    if (cropForegroundServiceIsRunning)
                        cropAppDate = stopCheckNoticePhoto(CROP_APP_NAME, cropAppDate)
                    else if (irrigationForegroundServiceIsRunning)
                        irrigationAppDate = stopCheckNoticePhoto(IRRIGATION_APP_NAME, irrigationAppDate)

                    val intentToForeground =
                        Intent(
                            this@MainService,
                            foregroundStatusTransmissionService::class.java
                        )

                    intentToForeground.putExtra(getString(R.string.id_user), idUser)
                    intentToForeground.putExtra(getString(R.string.message), message)
                    startService(intentToForeground)
                    sendPhotoInRunning = true
                }

                Actions.STOP_SEND_PHOTO -> {
                    if(verbose)  show(TAG, "Updated flag sendPhotoInRunning...")
                    sendPhotoInRunning = false

                    if (cropForegroundServiceIsRunning)
                        cropAppDate = stopCheckNoticePhoto(CROP_APP_NAME, cropAppDate)
                    else if (irrigationForegroundServiceIsRunning)
                        irrigationAppDate = stopCheckNoticePhoto(IRRIGATION_APP_NAME, irrigationAppDate)
                }
                else -> throw Exception("Actions not implemented!")
            }
        }
    }

    /**
     * Local receiver to communicate from foreground service to main service.
     */
    private val mainServiceReceiverFromForeground: BroadcastReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                val action =
                    intent.getParcelableExtra<Action>(context.getString(R.string.action))!!

                if(verbose)  show(
                    AidlServerServiceImpl.TAG,
                    "[Foreground --> Main Service] Received Action: $action"
                )

                when (action.action) {
                    Actions.ERROR_SEND_PHOTO, Actions.STOP_SEND_PHOTO -> {
                        if(verbose) show(TAG, "Updated flag sendPhotoInRunning...")
                        sendPhotoInRunning = false

                        if (cropForegroundServiceIsRunning)
                            stopCheckNoticePhoto(CROP_APP_NAME, cropAppDate)
                        else if (irrigationForegroundServiceIsRunning)
                            stopCheckNoticePhoto(IRRIGATION_APP_NAME, irrigationAppDate)
                    }

                    Actions.NOTIFY_PHOTO_SENT -> {}

                    else -> throw Exception("Actions not implemented!")
                }
            }
        }

    companion object {

        private const val PERIOD = 1000L

        // Notification Photo Time
        private const val NOTICE_HOUR_PHOTO = 13

        private const val START_CROP_NOTICE_MINUTE_PHOTO = 4
        private const val STOP_CROP_NOTICE_MINUTE_PHOTO = 5

        private const val START_IRRIGATION_NOTICE_MINUTE_PHOTO = 6
        private const val STOP_IRRIGATION_NOTICE_MINUTE_PHOTO = 7

        // Notification Debug time
        private const val DIFF_MINUTES_DEBUG = 200

        // App Name
        const val CROP_APP_NAME = "Colture"
        const val IRRIGATION_APP_NAME = "Irrigazione"

        // Logging
        private const val TAG = "Main Service"
        private const val verbose = true

        // Manage service
        private var sendPhotoInRunning = false
    }
}