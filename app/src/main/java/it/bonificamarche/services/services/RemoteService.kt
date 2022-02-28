package it.bonificamarche.services.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log
import it.bonificamarche.services.IRemoteService

class RemoteService : Service() {

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        show("Service created")
    }

    private val binder = object : IRemoteService.Stub() {

        override fun getPid(): Int {
            val pid = Process.myPid()
            show("*** New call: pid is $pid")
            return pid
        }

        override fun basicTypes(
            anInt: Int,
            aLong: Long,
            aBoolean: Boolean,
            aFloat: Float,
            aDouble: Double,
            aString: String
        ) {
        }
    }

    companion object {
        const val TAG = "RemoteService"
        val show = { log: String ->
            Log.e(TAG, log)
        }
    }
}