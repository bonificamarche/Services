package it.bonificamarche.services.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import it.bonificamarche.services.IRemoteService

class RemoteService : Service() {

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private val binder = object : IRemoteService.Stub() {

        override fun getPid(): Int {
            return Process.myPid()
        }

        override fun basicTypes(
            anInt: Int,
            aLong: Long,
            aBoolean: Boolean,
            aFloat: Float,
            aDouble: Double,
            aString: String
        ) { }
    }
}