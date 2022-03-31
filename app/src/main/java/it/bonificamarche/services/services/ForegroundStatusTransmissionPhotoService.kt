package it.bonificamarche.services.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import it.bonificamarche.services.Actions
import it.bonificamarche.services.R
import it.bonificamarche.services.aidl.Action
import it.bonificamarche.services.aidl.Photo
import it.bonificamarche.services.aidl.Transmission
import it.bonificamarche.services.common.encodePhoto
import it.bonificamarche.services.common.show
import java.io.File

class ForegroundStatusTransmissionPhotoService : Service() {

    override fun onBind(binder: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // Local Broadcast receiver to communicate
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(
                foregroundServiceReceiver,
                IntentFilter(getString(R.string.communicationFromAidlServerService))
            )

        val bundle = intent?.extras!!
        val message = bundle.getString(getString(R.string.message))!!
        val idUser = bundle.getInt(getString(R.string.id_user))

        createChannelNotification()
        sendPhotoToRemoteServer(idUser, message)
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
     * @param appName: app name.
     * @param imgToUpload: number of the image to uploaded.
     * @param progress: progress of status bar.
     */
    private fun createNotify(
        appName: String,
        imgToUpload: Int,
        progress: Int,
    ): Notification {

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Invio foto")
            .setContentText("$appName. Invio foto: $imgToUpload/$progress")
            .setProgress(imgToUpload, progress, false)
            .setSmallIcon(R.drawable.photo_library_icon)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    /**
     * Start service notice.
     */
    private fun startNoticeService(
        transmission: Transmission
    ) {
        if (verbose) show(TAG, "Start notice service")

        val src = transmission.src?.substring(0, transmission.src.length - 1)
        val appName = src?.split("/")?.last()
        show(TAG, "AppName : $appName, ${transmission.src}")

        val notification: Notification = createNotify(
            appName!!,
            transmission.photoToBeTransmitted,
            transmission.photoTransmitted + 1
        )

        startForeground(ON_GOING_NOTIFICATION_ID, notification)
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
                    startNoticeService(transmission)

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

            val typeFile =
                when {
                    path.contains(APP_NAME_COLTURE, ignoreCase = true) -> {
                        // Real estate photo
                        val splitting = nameFile.split("#")
                        if (splitting.size > 1) {
                            val id = splitting[0].split(REAL_ESTATE)
                            if (id.size > 1)
                                nameFile = id[1]
                        }
                        CROP
                    }
                    path.contains(APP_NAME_IRRIGAZIONE, ignoreCase = true) -> {
                        // Irrigation photo
                        val type: String = if (photo.name!![0] == '1') POINT
                        else READING

                        val splitting = nameFile.split(APP_NAME_IRRIGAZIONE)
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

                                    if (transmission.photoTransmitted == transmission.photoToBeTransmitted)
                                        stopService(Actions.STOP_SEND_PHOTO, transmission, photo)
                                },
                                    { error ->
                                        show(TAG, "Error in upload photo: ${error.message}")
                                        sendMessageToAidlServerToNotifyPhoto(
                                            Actions.ERROR_SEND_PHOTO,
                                            transmission, photo, error.message!!
                                        )
                                        stopService(Actions.ERROR_SEND_PHOTO, transmission, photo, error.message!!)
                                    })
                        } else {
                            show(TAG, "[Transmission stopped]")
                            stopService(Actions.STOP_SEND_PHOTO, transmission, photo)
                        }
                    },
                        { error ->
                        show(TAG, "Error in upload photo: ${error.message}")
                        sendMessageToAidlServerToNotifyPhoto(
                            Actions.ERROR_SEND_PHOTO,
                            transmission, photo, error.message!!
                        )
                        stopService(Actions.ERROR_SEND_PHOTO, transmission, photo, error.message!!)
                    })

            } else{
                show(TAG, "[Transmission stopped]")
                stopService(Actions.STOP_SEND_PHOTO, transmission, photo)
            }

        } catch (error: Exception) {
            show(TAG, "Error in upload photo: ${error.message}")
            sendMessageToAidlServerToNotifyPhoto(
                Actions.ERROR_SEND_PHOTO,
                transmission, photo, error.message!!
            )
            stopService(Actions.ERROR_SEND_PHOTO, transmission, photo, error.message!!)
        }
    }


    private fun stopService(action: Actions, transmission: Transmission, photo: Photo, message : String = "") {
        sendMessageToAidlServerToNotifyPhoto(action, transmission, photo, message)
        this.stopSelf()
        onDestroy()
    }

    /**
     * Local receiver to communicate from aidl server service to this service.
     */
    private val foregroundServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras!!
            val action = intent.getSerializableExtra(context.getString(R.string.action)) as Actions
            val message = bundle.getString(context.getString(R.string.message))!!

            show(
                TAG,
                "[AIDL Server --> Foreground status transmission photo] Received Action: $action, message: $message"
            )

            when (action) {
                Actions.STOP_SEND_PHOTO -> {
                    show(TAG, "Updated flag sendPhotoInRunning...")
                    sendPhotoInRunning = false
                }

                Actions.START_SEND_PHOTO -> {}
                else -> throw Exception("Actions not implemented!")
            }
        }
    }

    /**
     * Call this function when the service needs to communicate with the server service to notify.
     * @param action: action to run.
     * @param transmission: transmission status.
     * @param photo: information on the last photo transmitted.
     * @param message: content of the message.

     */
    private fun sendMessageToAidlServerToNotifyPhoto(
        action: Actions, transmission: Transmission, photo: Photo,
        message: String = ""
    ) {
        val intent = Intent(getString(R.string.communicationForegroundSendPhoto))
        intent.putExtra(getString(R.string.action), Action(action))
        intent.putExtra(getString(R.string.message), message)
        intent.putExtra(getString(R.string.transmission), transmission)
        intent.putExtra(getString(R.string.photo), photo)
        show(
            TAG,
            "[Foreground status transmission photo --> AIDL Server]  Action: $action, message: $message"
        )

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    companion object {

        private var sendPhotoInRunning = false

        // Asynchronous requests
        private val apiService by lazy {
            IApiServices.create("https://webserv.bonificamarche.it")
        }

        private val apiServiceResizeImage by lazy {
            IApiServices.create("https://cdn.bonificamarche.it")
        }

        // Logging
        private const val TAG = "Foreground Status Transmission Photo Service"
        private const val verbose = true

        // Notification
        private const val CHANNEL_ID = "Notifying the photo status transmission"
        private const val ON_GOING_NOTIFICATION_ID = 1

        // Upload photo to remote server
        private const val FOLDER_NAME_SERVER = "IT02532390412"
        private const val CROP = "coltura"
        private const val REAL_ESTATE = "Immobile"
        private const val POINT = "punto"
        private const val READING = "lettura"
        private const val UNKNOWN = "Sconosciuto"

        // App Name
        private const val APP_NAME_COLTURE = "Colture"
        private const val APP_NAME_IRRIGAZIONE = "Irrigazione"
    }
}