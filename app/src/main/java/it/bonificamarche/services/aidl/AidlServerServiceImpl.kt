package it.bonificamarche.services.aidl

import android.content.*
import android.os.IBinder
import android.os.Process
import android.os.RemoteCallbackList
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.bonificamarche.services.*
import it.bonificamarche.services.common.show
import it.bonificamarche.services.services.Actions
import it.bonificamarche.services.services.MainService
import java.lang.Exception

class AidlServerServiceImpl(private val context : Context) : IAidlServerService.Stub() {

    // Main Service Property
    private var mainService: MainService? = null
    private var boundMainService: Boolean = false

    private val callbacks: RemoteCallbackList<IAidlServerServiceCallback> =
        RemoteCallbackList<IAidlServerServiceCallback>()

    /**
     * Like the on create function.
     */
    fun initialization() {

        // Main Service
        Intent(context, MainService::class.java).also { intent ->
            context.bindService(intent, connectionMainService, Context.BIND_AUTO_CREATE)
        }

        // Local Broadcast receiver to communicate
        LocalBroadcastManager.getInstance(context)
            .registerReceiver(aidlServerReceiver,
                IntentFilter(context.getString(R.string.communicationFromMainServiceToAidlServerService)))
    }

    /**
     * Like the on destroy function.
     */
    fun destroyResources(){
        if (boundMainService) {
            context.unbindService(connectionMainService)
            boundMainService = false
        }
    }

    /**
     * Implement a service connection to connect with the main service.
     */
    private val connectionMainService: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            show(TAG, "Binding with main service!")
            val binder = service as MainService.LocalBinder
            mainService = binder.getService()
            boundMainService = true

            context.startService(Intent(context, MainService::class.java)).also {
                show(TAG, "Main service starting...")
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            show(TAG, "Unbinding with main service!")
            boundMainService = false
            mainService = null
        }
    }

    override fun getPid(): Int {
        val pid = Process.myPid()
        show(TAG, "*** New call: pid is $pid")
        return pid
    }

    override fun sendPhoto(path: String) {
        show(TAG, "*** New call. Send photo!")
        sendMessageToMainService(Actions.SEND_PHOTO)
    }

    override fun notifyClient(notifyContent: String) {
        callbacks.beginBroadcast()

        for (i in 0 until callbacks.registeredCallbackCount) {
            val cb: IAidlServerServiceCallback = callbacks.getBroadcastItem(i)

            val transmission = Transmission("", 0, 0)
            val photo = Photo(notifyContent, "")
            cb.sendStatusTransmissionPhoto(transmission, photo)
        }

        callbacks.finishBroadcast()
    }

    override fun registerCallback(cb: IAidlServerServiceCallback) {
        show(TAG, "Register callback: $cb, callbacks = $callbacks")
        callbacks.register(cb)
    }

    override fun unregisterCallback(cb: IAidlServerServiceCallback) {
        show(TAG, "Unregister callback.")
        callbacks.unregister(cb)
    }

    /**
     * Local receiver to communicate from main service to aidl server service.
     */
    private val aidlServerReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            val action = intent.getSerializableExtra(context.getString(R.string.action)) as Actions
            val message = bundle?.getString(context.getString(R.string.message))!!
            show(TAG, "[Main Service --> AIDL Server] Action: $action, message: $message")

            when(action){
                Actions.NOTIFY_CLIENTS -> notifyClient(message)
                else -> throw Exception("Actions not implemented!")
            }
        }
    }

    /**
     * Call this function when the aidl server needs to communicate with the main service.
     * @param message: content of the message.
     */
    private fun sendMessageToMainService(action: Actions, message : String = "") {
        val intent = Intent(context.getString(R.string.communicationFromAidlServerServiceToMainService))
        intent.putExtra(context.getString(R.string.action), action)
        intent.putExtra(context.getString(R.string.message), message)
        show(TAG, "[AIDL Server --> Main Service] Action: $action, message: $message.")

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    companion object {
        const val TAG = "AidlServerServiceImpl"
    }
}