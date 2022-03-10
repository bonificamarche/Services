package it.bonificamarche.services.aidl

import android.os.Process
import android.os.RemoteCallbackList
import it.bonificamarche.services.IAidlServerService
import it.bonificamarche.services.IAidlServerServiceCallback
import it.bonificamarche.services.Photo
import it.bonificamarche.services.Transmission
import it.bonificamarche.services.services.AidlServerService

class AidlServerServiceImpl: IAidlServerService.Stub() {

    private val callbacks: RemoteCallbackList<IAidlServerServiceCallback> =
        RemoteCallbackList<IAidlServerServiceCallback>()

    override fun getPid(): Int {
        val pid = Process.myPid()
        AidlServerService.show("*** New call: pid is $pid")
        return pid
    }

    override fun sendPhoto(path: String) {
        AidlServerService.show("*** New call. Send photo!")

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
        AidlServerService.show("Register callback: $cb, callbacks = $callbacks")
        callbacks.register(cb)
    }

    override fun unregisterCallback(cb: IAidlServerServiceCallback) {
        AidlServerService.show("Unregister callback.")
        callbacks.unregister(cb)
    }
}