package it.bonificamarche.services.aidl

import android.content.*
import android.os.IBinder
import android.os.Process
import android.os.RemoteCallbackList
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import it.bonificamarche.services.*
import it.bonificamarche.services.common.show
import it.bonificamarche.services.services.AidlServerService
import it.bonificamarche.services.services.MainService

class AidlServerServiceImpl(private val context : Context) : IAidlServerService.Stub() {

    // Main Service Property
    private var mainService: MainService? = null
    private var boundMainService: Boolean = false

    private val callbacks: RemoteCallbackList<IAidlServerServiceCallback> =
        RemoteCallbackList<IAidlServerServiceCallback>()

    fun initialization() {

        Intent(context, MainService::class.java).also { intent ->
            context.bindService(intent, connectionMainService, Context.BIND_AUTO_CREATE)
        }

        LocalBroadcastManager.getInstance(context)
            .registerReceiver(communicationServicesReceiver, IntentFilter(context.getString(R.string.communication)))
    }

    fun destroyResources(){
        if (boundMainService) {
            context.unbindService(connectionMainService)
            boundMainService = false
        }
    }

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

        for (i in 0 until 5) {
            Thread.sleep(2000)
            notifyClient("Sent photo ${i + 1}")
        }
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
     * Local receiver to communicate between aidl server and main service.
     */
    private val communicationServicesReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            val message = bundle?.getString(context.getString(R.string.message))
            show(TAG, "Received message: $message from main service")
        }
    }

    companion object {
        const val TAG = "AidlServerServiceImpl"
    }
}