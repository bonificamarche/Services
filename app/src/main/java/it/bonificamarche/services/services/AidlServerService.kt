package it.bonificamarche.services.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.os.RemoteCallbackList
import android.util.Log
import it.bonificamarche.services.IAidlServerService
import it.bonificamarche.services.IAidlServerServiceCallback


class AidlServerService : Service() {

    private val callbacks: RemoteCallbackList<IAidlServerServiceCallback> =
        RemoteCallbackList<IAidlServerServiceCallback>()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        show("Service created")
    }

    private val binder = object : IAidlServerService.Stub() {

        override fun getPid(): Int {
            val pid = Process.myPid()
            show("*** New call: pid is $pid")
            return pid
        }

        override fun sendPhoto(path: String) {
           for(i in 0 until 5){
               Thread.sleep(2000)
               notifyClient("Sent photo ${i+1}")
           }
        }

        override fun notifyClient(notifyContent: String) {
            callbacks.beginBroadcast()

            for (i in 0 until callbacks.registeredCallbackCount) {
                val cb: IAidlServerServiceCallback = callbacks.getBroadcastItem(i)
                cb.sendMsg(notifyContent)
            }

            callbacks.finishBroadcast()
        }

        override fun registerCallback(cb: IAidlServerServiceCallback) {
            show("Register callback: $cb, callbacks = $callbacks")
            callbacks.register(cb)
        }

        override fun unregisterCallback(cb: IAidlServerServiceCallback) {
            show("Unregister callback.")
            callbacks.unregister(cb)
        }
    }

    companion object {
        private const val TAG = "AidlServerService"
        val show = { log: String ->
            Log.e(TAG, log)
        }
    }
}