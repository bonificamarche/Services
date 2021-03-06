package it.bonificamarche.services.services

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import it.bonificamarche.services.Actions
import it.bonificamarche.services.R
import it.bonificamarche.services.aidl.Action
import it.bonificamarche.services.aidl.Photo
import it.bonificamarche.services.aidl.Transmission
import it.bonificamarche.services.common.*
import java.io.File
import java.util.*

open class MainService : Service() {

    private val binder = LocalBinder()

    // Asynchronous requests
    private val apiService by lazy {
        IApiServices.create("https://webserv.bonificamarche.it")
    }

    private val apiServiceResizeImage by lazy {
        IApiServices.create("https://cdn.bonificamarche.it")
    }

    // Timer
    private var timer: Timer? = null
    var cropAppDate: String = getParsedDate(getLocalDate())
    var irrigationAppDate: String = getParsedDate(getLocalDate())
    var currentTime = Calendar.getInstance().time

    // Foreground photo
    private lateinit var foregroundPhotoService: ForegroundPhotoService
    private var cropForegroundServiceIsRunning = false
    private var irrigationForegroundServiceIsRunning = false

    private var sendPhotoInRunning = false

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
                            START_CROP_NOTICE_MINUTE_PHOTO -> {
                                startCheckNoticePhoto(CROP_APP_NAME, cropAppDate)
                            }

                            STOP_CROP_NOTICE_MINUTE_PHOTO -> {
                                cropAppDate = stopCheckNoticePhoto(cropAppDate)
                            }

                            START_IRRIGATION_NOTICE_MINUTE_PHOTO -> {
                                startCheckNoticePhoto(IRRIGATION_APP_NAME, irrigationAppDate)
                            }

                            STOP_IRRIGATION_NOTICE_MINUTE_PHOTO -> {
                                irrigationAppDate = stopCheckNoticePhoto(irrigationAppDate)
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
        if (verbose) show(TAG, "[Check Photo] in progress... Found: $imgToSend")

        if (compareDate(date, dateToCheck) && !sendPhotoInRunning) {

            if (imgToSend > 0) {

                if (verbose) show(TAG, "[Check Photo] There are photos to send.!")

                if (!flag) {
                    // Start services
                    val intent = Intent(this, foregroundPhotoService::class.java)
                    intent.putExtra(getString(R.string.AppName), appName)

                    show(TAG, "[Check Photo] Foreground service starting...")
                    startService(intent)
                    flag = true
                }
            }
        } else if (verbose) show(
            TAG,
            "[Check Photo] Wait new date... for check photo. Current is $cropAppDate and check is $dateToCheck"
        )

        if (appName == IRRIGATION_APP_NAME)
            irrigationForegroundServiceIsRunning = flag
        else
            cropForegroundServiceIsRunning = flag
    }

    /**
     * Stop the foreground photo service.
     */
    private fun stopCheckNoticePhoto(date: String): String {

        val newDate = addOneDay(date)

        if (cropForegroundServiceIsRunning || irrigationForegroundServiceIsRunning) {
            if (verbose) show(TAG, "Foreground service stopping...")
            stopService(Intent(this@MainService, foregroundPhotoService::class.java))

            if (verbose) show(TAG, "Reset parameters. New date is $newDate")
            cropForegroundServiceIsRunning = false
            irrigationForegroundServiceIsRunning = false
        }

        return newDate
    }

    /**
     * Send photo to remote server.
     * @param idUser: id user server to sent photo.
     * @param path: path of the root folder that contains the photos to be sent.
     */
    private fun sendPhotoToRemoteServer(idUser: Int, path: String) {

        val folder = File(path)
        if (folder.exists() && folder.isDirectory) {

            val photoToTransmit = folder.listFiles()!!.size
            val transmission = Transmission(path, photoToTransmit, 0)
            folder.listFiles()?.forEachIndexed { index, file ->

                if (index == 0) sendPhotoInRunning = true   // Start transmission

                if (sendPhotoInRunning) {

                    val photo = Photo(file.name, file.absolutePath)
                    if (verbose) show(TAG, "Encoding.....")
                    val encodedPhoto = encodePhoto(photo.fullName!!)

                    if (verbose) show(TAG, "Photo encoded! Start to upload.")
                    uploadPhoto(path, idUser, encodedPhoto, transmission, photo)
                }
            }
        }
    }

    /**
     * Upload photo to the server.
     * @param path: path to search for photos.
     * @param idUser: id user server to sent photo.
     * @param encoded: photo in base64 to upload.
     * @param transmission: status of the transmission.
     * @param photo: current photo to be transmit.
     */
    @SuppressLint("CheckResult")
    private fun uploadPhoto(
        path: String,
        idUser: Int,
        encoded: String,
        transmission: Transmission,
        photo: Photo
    ) {

        try {
            var nameFile = photo.fullName!!.replace(".png", "").substring(1)
            // TODO Check this when adds new labels photo
            val typeFile =
                when {
                    path.contains(CROP_APP_NAME, ignoreCase = true) -> {
                        // Real estate photo
                        val splitting = nameFile.split("#")
                        if (splitting.size > 1) {
                            val id = splitting[0].split(REAL_ESTATE)
                            if (id.size > 1)
                                nameFile = id[1]
                        }
                        CROP
                    }
                    path.contains(IRRIGATION_APP_NAME, ignoreCase = true) -> {
                        // Irrigation photo
                        val type: String = if (photo.name!![0] == '1') POINT
                        else READING

                        val splitting = nameFile.split(IRRIGATION_APP_NAME)
                        if (splitting.size > 1) {
                            nameFile = splitting[1].substring(1)

                            val tempName = nameFile.split("#")
                            nameFile = tempName[0].substring(1) + "#" + tempName[1]
                        }
                        type
                    }
                    else -> UNKNOWN
                }

            // Upload photo to the server
            if (verbose) show(TAG, "Start upload name: $nameFile, type: $typeFile")

            if (sendPhotoInRunning) {
                apiService.uploadPhoto(
                    getString(R.string.kk),
                    getString(R.string.ks),
                    idUser.toString(),
                    typeFile,
                    nameFile,
                    encoded
                )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .retry { retryCount, _ -> retryCount < 3 }
                    .subscribe({ json ->

                        val nameFileServer = json.get("file").toString().replace("\"", "")
                        if (verbose)
                            show(
                                TAG,
                                "Result received: $nameFileServer!\nSend request to image resized."
                            )

                        // Start resizing
                        if (verbose) show(TAG, "Start resizing...")

                        if (sendPhotoInRunning) {
                            apiServiceResizeImage.resizeImages(FOLDER_NAME_SERVER, nameFileServer)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .retry { retryCount, _ -> retryCount < 3 }
                                .subscribe({

                                    if (verbose) show(TAG, "Resize completed!")
                                    transmission.photoTransmitted += 1

                                    // Notify clients
                                    sendMessageToAidlServerToNotifyPhoto(
                                        Actions.NOTIFY_PHOTO_SENT, transmission,
                                        photo, "Send photo: ${photo.name}"
                                    )
                                },
                                    { error ->
                                        show(TAG, "Error in upload photo: ${error.message}")
                                        sendMessageToAidlServerToNotifyPhoto(
                                            Actions.ERROR_SEND_PHOTO,
                                            transmission, photo, error.message!!
                                        )

                                    })
                        } else show(TAG, "[Transmission stopped]")
                    }, { error ->
                        show(TAG, "Error in upload photo: ${error.message}")
                        sendMessageToAidlServerToNotifyPhoto(
                            Actions.ERROR_SEND_PHOTO,
                            transmission, photo, error.message!!
                        )
                    })
            } else show(TAG, "[Transmission stopped]")
        } catch (error: Exception) {
            show(TAG, "Error in upload photo: ${error.message}")
            sendMessageToAidlServerToNotifyPhoto(
                Actions.ERROR_SEND_PHOTO,
                transmission, photo, error.message!!
            )
        }
    }

    /**
     * Local receiver to communicate from aidl server service to main service.
     */
    private val mainServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras!!
            val action = intent.getSerializableExtra(context.getString(R.string.action)) as Actions
            val message = bundle.getString(context.getString(R.string.message))!!
            val idUser = bundle.getInt(context.getString(R.string.id_user))

            show(TAG, "[AIDL Server --> Main Service] Received Action: $action, message: $message")

            when (action) {
                Actions.START_SEND_PHOTO -> {

                    if (cropForegroundServiceIsRunning)
                        stopCheckNoticePhoto(cropAppDate)
                    else if (irrigationForegroundServiceIsRunning)
                        stopCheckNoticePhoto(irrigationAppDate)

                    sendPhotoToRemoteServer(idUser, message)
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
     * Call this function when the main service needs to communicate with the server service to notify.
     * @param action: action to run.
     * @param transmission: transmission status.
     * @param photo: information on the last photo transmitted.
     * @param message: content of the message.

     */
    private fun sendMessageToAidlServerToNotifyPhoto(
        action: Actions, transmission: Transmission, photo: Photo,
        message: String = ""
    ) {
        val intent = Intent(getString(R.string.communicationFromMainServiceToAidlServerService))
        intent.putExtra(getString(R.string.action), Action(action))
        intent.putExtra(getString(R.string.message), message)
        intent.putExtra(getString(R.string.transmission), transmission)
        intent.putExtra(getString(R.string.photo), photo)
        show(TAG, "[Main Service --> AIDL Server]  Action: $action, message: $message")

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {

        private const val PERIOD = 1000L

        // Notification Photo Time
        private const val NOTICE_HOUR_PHOTO = 11

        private const val START_CROP_NOTICE_MINUTE_PHOTO = 8
        private const val STOP_CROP_NOTICE_MINUTE_PHOTO = 47

        private const val START_IRRIGATION_NOTICE_MINUTE_PHOTO = 48
        private const val STOP_IRRIGATION_NOTICE_MINUTE_PHOTO = 49

        // Notification Debug time
        private const val DIFF_MINUTES_DEBUG = 200

        // App Name
        const val CROP_APP_NAME = "Colture"
        const val IRRIGATION_APP_NAME = "Irrigazione"

        // Upload photo to remote server
        const val FOLDER_NAME_SERVER = "IT02532390412"
        const val CROP = "coltura"
        const val REAL_ESTATE = "Immobile"
        const val POINT = "punto"
        const val READING = "lettura"
        const val UNKNOWN = "Sconosciuto"

        // Logging
        const val TAG = "Main Service"
        const val verbose = true
    }
}